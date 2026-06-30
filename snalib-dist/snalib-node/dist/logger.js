export class SnaLibLogger {
    logEnabled;
    onWarning;
    constructor(logEnabled, onWarning) {
        this.logEnabled = logEnabled;
        this.onWarning = onWarning;
    }
    info(msg, ...kv) {
        if (!this.logEnabled)
            return;
        process.stderr.write(this.buildJson('info', msg, kv) + '\n');
    }
    warn(msg, ...kv) {
        const json = this.buildJson('warn', msg, kv);
        if (this.onWarning !== undefined) {
            // Pass the full JSON entry — same string that would otherwise go to stderr.
            this.onWarning(json);
            return;
        }
        process.stderr.write(json + '\n');
    }
    buildJson(level, msg, kv) {
        const entry = { level, msg };
        for (let i = 0; i + 1 < kv.length; i += 2) {
            const key = String(kv[i]);
            entry[key] = kv[i + 1];
        }
        return JSON.stringify(entry);
    }
}
