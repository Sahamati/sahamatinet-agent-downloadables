export class SnaLibLogger {
  private readonly logEnabled: boolean;
  private readonly onWarning: ((msg: string) => void) | undefined;

  constructor(logEnabled: boolean, onWarning?: (msg: string) => void) {
    this.logEnabled = logEnabled;
    this.onWarning = onWarning;
  }

  info(msg: string, ...kv: unknown[]): void {
    if (!this.logEnabled) return;
    process.stderr.write(this.buildJson('info', msg, kv) + '\n');
  }

  warn(msg: string, ...kv: unknown[]): void {
    const json = this.buildJson('warn', msg, kv);
    if (this.onWarning !== undefined) {
      // Pass the full JSON entry — same string that would otherwise go to stderr.
      this.onWarning(json);
      return;
    }
    process.stderr.write(json + '\n');
  }

  private buildJson(level: string, msg: string, kv: unknown[]): string {
    const entry: Record<string, unknown> = { level, msg };
    for (let i = 0; i + 1 < kv.length; i += 2) {
      const key = String(kv[i]);
      entry[key] = kv[i + 1];
    }
    return JSON.stringify(entry);
  }
}
