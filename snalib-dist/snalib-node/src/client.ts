import { SNAConnectionError, SNANotImplementedError, SNAServerError, SNAValidationError } from './errors.js';
import { SnaLibLogger } from './logger.js';
import { SNAConfig, VersionResponse, type ErrorPayload } from './models.js';
import { EntityClient } from './entity.js';
import { AAClient } from './aa.js';

const DEFAULT_MAX_RETRIES = 3;
const DEFAULT_RETRY_INTERVAL_MS = 1000;
const DEFAULT_MAX_WAIT_MS = 15000;
const DEFAULT_TIMEOUT_MS = 30000;

const sleep = (ms: number): Promise<void> => new Promise(r => setTimeout(r, ms));

export class SNAClient {
  private readonly baseUrl: string;
  private readonly timeout: number;
  private readonly logger: SnaLibLogger;

  readonly entity: EntityClient;
  readonly aa: AAClient;

  private constructor(config: SNAConfig, logger: SnaLibLogger) {
    this.baseUrl = config.baseUrl.replace(/\/$/, '');
    this.timeout = config.timeout ?? DEFAULT_TIMEOUT_MS;
    this.logger = logger;
    this.entity = new EntityClient(this);
    this.aa = new AAClient(this);
  }

  static async create(config: SNAConfig): Promise<SNAClient> {
    try {
      const logger = new SnaLibLogger(config.logEnabled ?? false, config.onWarning);
      const client = new SNAClient(config, logger);

      const maxRetries = config.maxRetries ?? DEFAULT_MAX_RETRIES;
      const retryInterval = config.retryInterval ?? DEFAULT_RETRY_INTERVAL_MS;
      const maxWait = config.maxWait ?? DEFAULT_MAX_WAIT_MS;

      try {
        await client.connectWithRetry(maxRetries, retryInterval, maxWait);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : String(err);
        logger.warn(msg);
      }

      return client;
    } catch (err: unknown) {
      // Outer catch: should never reach here, but satisfies the never-reject contract.
      const msg = err instanceof Error ? err.message : String(err);
      const fallbackLogger = new SnaLibLogger(false);
      fallbackLogger.warn(`SNAClient.create unexpected error: ${msg}`);
      // Return a minimally functional client so callers are never left without one.
      const safeConfig: SNAConfig = { ...config };
      return new SNAClient(safeConfig, fallbackLogger);
    }
  }

  private async connectWithRetry(maxRetries: number, retryInterval: number, maxWait: number): Promise<void> {
    const deadline = Date.now() + maxWait;
    let attempt = 0;
    let interval = retryInterval;

    while (attempt <= maxRetries) {
      try {
        await this.doPing();
        return;
      } catch {
        attempt++;
        if (attempt > maxRetries) break;

        if (Date.now() + interval > deadline) break;
        await sleep(interval);
        interval *= 2;
      }
    }

    throw new SNAConnectionError(`service unreachable after ${attempt} attempt(s): ${this.baseUrl}`);
  }

  private async doPing(): Promise<void> {
    const [status] = await this.doGet('/sna/v1/ping');
    if (status !== 200) {
      throw new SNAConnectionError(`ping returned HTTP ${status}`);
    }
  }

  async ping(): Promise<boolean> {
    try {
      await this.doPing();
      return true;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      this.logger.warn(msg);
      return false;
    }
  }

  async version(): Promise<VersionResponse> {
    try {
      const [status, body] = await this.doGet('/sna/v1/version');
      if (status !== 200) {
        this.parseError(status, body);
      }
      const data = this.parseJson(body);
      return VersionResponse.from(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      this.logger.warn(msg);
      return VersionResponse.error(msg);
    }
  }

  // Package-internal helpers used by EntityClient and AAClient.

  logWarn(msg: string): void {
    this.logger.warn(msg);
  }

  logInfo(msg: string): void {
    this.logger.info(msg);
  }

  async doGet(path: string): Promise<[number, string]> {
    const url = `${this.baseUrl}${path}`;
    let resp: Response;
    try {
      resp = await fetch(url, { signal: AbortSignal.timeout(this.timeout) });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      throw new SNAConnectionError(msg);
    }
    const text = await resp.text();
    return [resp.status, text];
  }

  async doPost(path: string, body: string): Promise<[number, string]> {
    const url = `${this.baseUrl}${path}`;
    let resp: Response;
    try {
      resp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
        signal: AbortSignal.timeout(this.timeout),
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      throw new SNAConnectionError(msg);
    }
    const text = await resp.text();
    return [resp.status, text];
  }

  parseError(status: number, rawBody: string): never {
    let payload: ErrorPayload = {};
    try {
      payload = JSON.parse(rawBody) as ErrorPayload;
    } catch {
      // best-effort; leave payload empty
    }
    const message = payload.message ?? rawBody;
    const errorCode = payload.errorCode ?? String(status);

    if (status === 400) throw new SNAValidationError(status, message, errorCode);
    if (status === 501) throw new SNANotImplementedError(status, message, errorCode);
    throw new SNAServerError(status, message, errorCode);
  }

  parseJson(body: string): Record<string, unknown> {
    try {
      return JSON.parse(body) as Record<string, unknown>;
    } catch {
      throw new SNAConnectionError(`failed to parse response body as JSON: ${body}`);
    }
  }
}
