import { describe, expect, it } from "vitest";

import {
  clientIpHash,
  fromB64url,
  hmacHex,
  json,
  normalizeCode,
  randomCode,
  readJson,
  sha256Hex,
  timingSafeEqual,
  toB64url,
  toHex,
  validAccountId,
  validAuthKey,
  validNonce,
  validPublicKey,
  allowRate,
} from "../functions/_lib/core.js";

describe("base64url", () => {
  it("round-trips arbitrary bytes", () => {
    const bytes = new Uint8Array([0x04, 0x00, 0xff, 0x7f, 0x80, 0x2a]);
    const text = toB64url(bytes);
    expect(fromB64url(text)).toEqual(bytes);
  });

  it("rejects malformed input", () => {
    expect(fromB64url("!!!")).toBeNull();
    expect(fromB64url("a")).toBeNull(); // length % 4 === 1
    expect(fromB64url(12)).toBeNull();
    expect(fromB64url("")).toBeNull();
  });
});

describe("hashing", () => {
  it("sha256 matches the known vector", async () => {
    expect(await sha256Hex("abc")).toBe(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
    );
  });

  it("hmac-sha256 matches the known vector", async () => {
    expect(await hmacHex("key", "The quick brown fox jumps over the lazy dog")).toBe(
      "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
    );
  });

  it("hex encodes with padding", () => {
    expect(toHex(new Uint8Array([0, 15, 255]))).toBe("000fff");
  });

  it("timingSafeEqual compares properly", () => {
    expect(timingSafeEqual("deadbeef", "deadbeef")).toBe(true);
    expect(timingSafeEqual("deadbeef", "deadbeee")).toBe(false);
    expect(timingSafeEqual("deadbeef", "deadbee")).toBe(false);
    expect(timingSafeEqual(null, "deadbeef")).toBe(false);
  });
});

describe("share codes", () => {
  it("normalizes case and separators", () => {
    expect(normalizeCode("km4-z7q")).toBe("KM4Z7Q");
    expect(normalizeCode(" KM4 Z7Q ")).toBe("KM4Z7Q");
  });

  it("rejects confusable and wrong-length codes", () => {
    expect(normalizeCode("KM4Z7O")).toBeNull(); // O not in alphabet
    expect(normalizeCode("KM4Z7I")).toBeNull(); // I not in alphabet
    expect(normalizeCode("KM4Z70")).toBeNull(); // 0 not in alphabet
    expect(normalizeCode("KM4Z7")).toBeNull();
    expect(normalizeCode(42)).toBeNull();
  });

  it("generates valid codes", () => {
    for (let i = 0; i < 50; i++) {
      const code = randomCode();
      expect(code).toHaveLength(6);
      expect(normalizeCode(code)).toBe(code);
    }
  });
});

describe("credential shapes", () => {
  it("accepts 16-byte account ids", () => {
    expect(validAccountId(toB64url(new Uint8Array(16)))).toBe(true);
    expect(validAccountId(toB64url(new Uint8Array(15)))).toBe(false);
  });

  it("accepts 32-byte auth keys", () => {
    expect(validAuthKey(toB64url(new Uint8Array(32)))).toBe(true);
    expect(validAuthKey(toB64url(new Uint8Array(31)))).toBe(false);
  });

  it("accepts only uncompressed 65-byte points", () => {
    const point = new Uint8Array(65);
    point[0] = 4;
    expect(validPublicKey(toB64url(point))).toBe(true);
    point[0] = 3;
    expect(validPublicKey(toB64url(point))).toBe(false);
    expect(validPublicKey(toB64url(new Uint8Array(64)))).toBe(false);
  });

  it("accepts 12-byte nonces", () => {
    expect(validNonce(toB64url(new Uint8Array(12)))).toBe(true);
    expect(validNonce(toB64url(new Uint8Array(11)))).toBe(false);
  });
});

describe("readJson", () => {
  const makeRequest = (body, headers = {}) =>
    new Request("https://example.com/x", { method: "POST", body, headers });

  it("parses object bodies", async () => {
    expect(await readJson(makeRequest('{"a":1}'))).toEqual({ a: 1 });
  });

  it("rejects bad JSON, arrays and oversize bodies", async () => {
    expect(await readJson(makeRequest("nope"))).toBeNull();
    expect(await readJson(makeRequest("[1,2]"))).toBeNull();
    expect(await readJson(makeRequest('{"a":"' + "x".repeat(100) + '"}'), 32)).toBeNull();
  });
});

describe("clientIpHash", () => {
  it("hashes the connecting ip deterministically", async () => {
    const one = await clientIpHash(
      new Request("https://example.com", { headers: { "CF-Connecting-IP": "1.2.3.4" } }),
    );
    const two = await clientIpHash(
      new Request("https://example.com", { headers: { "CF-Connecting-IP": "1.2.3.4" } }),
    );
    const three = await clientIpHash(
      new Request("https://example.com", { headers: { "CF-Connecting-IP": "5.6.7.8" } }),
    );
    expect(one).toBe(two);
    expect(one).not.toBe(three);
    expect(one).toHaveLength(64);
  });
});

/** Minimal D1 stub covering exactly the statements allowRate() runs. */
function stubD1() {
  const rows = new Map();
  return {
    rows,
    prepare(sql) {
      return {
        bind(...args) {
          return {
            async first() {
              if (sql.includes("FROM rate_limits WHERE key")) {
                const row = rows.get(args[0]);
                return row ? { window_start: row.window_start, count: row.count } : null;
              }
              throw new Error("stub: unexpected first(): " + sql);
            },
            async run() {
              if (sql.includes("INSERT INTO rate_limits")) {
                rows.set(args[0], { window_start: args[1], count: 1 });
                return { success: true };
              }
              if (sql.includes("UPDATE rate_limits")) {
                const row = rows.get(args[0]);
                if (row) row.count += 1;
                return { success: true };
              }
              throw new Error("stub: unexpected run(): " + sql);
            },
          };
        },
      };
    },
  };
}

describe("allowRate", () => {
  it("allows up to the limit inside a window, then blocks", async () => {
    const env = { DB: stubD1() };
    for (let i = 0; i < 3; i++) {
      expect(await allowRate(env, "k", 3, 60_000)).toBe(true);
    }
    expect(await allowRate(env, "k", 3, 60_000)).toBe(false);
  });

  it("resets the window after it expires", async () => {
    const env = { DB: stubD1() };
    const realNow = Date.now;
    let now = 1_000_000;
    Date.now = () => now;
    try {
      expect(await allowRate(env, "k", 1, 1_000)).toBe(true);
      expect(await allowRate(env, "k", 1, 1_000)).toBe(false);
      now += 2_000;
      expect(await allowRate(env, "k", 1, 1_000)).toBe(true);
    } finally {
      Date.now = realNow;
    }
  });
});

describe("json responses", () => {
  it("sets no-store and nosniff", async () => {
    const response = json({ ok: true }, 201);
    expect(response.status).toBe(201);
    expect(response.headers.get("cache-control")).toBe("no-store");
    expect(response.headers.get("x-content-type-options")).toBe("nosniff");
    expect(await response.json()).toEqual({ ok: true });
  });
});
