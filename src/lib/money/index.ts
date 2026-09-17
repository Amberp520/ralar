/**
 * Ralar Authoritative Money Module (Stellar 7 Decimals Precision)
 */

export const DECIMALS = 7;
export const SCALE = 10_000_000n; // 10^7

export const toMinor = (human: string): bigint => {
  const trimmed = human.trim();
  if (!trimmed) throw new Error("Amount cannot be empty");
  if (trimmed.startsWith("-")) throw new Error("Amount cannot be negative");

  const parts = trimmed.split(".");
  if (parts.length > 2) throw new Error("Invalid decimal string: multiple decimal points");

  const integerPart = parts[0] || "0";
  if (!/^\d+$/.test(integerPart)) {
    throw new Error(`Invalid characters in integer portion: ${integerPart}`);
  }

  const wholeUnits = BigInt(integerPart) * SCALE;

  if (parts.length === 1) {
    return wholeUnits;
  }

  const fractionalPart = parts[1];
  if (!/^\d+$/.test(fractionalPart)) {
    throw new Error(`Invalid characters in fractional portion: ${fractionalPart}`);
  }

  if (fractionalPart.length > DECIMALS) {
    throw new Error("Exceeds maximum precision: Stellar supports at most 7 decimal places");
  }

  const paddedFraction = fractionalPart.padEnd(DECIMALS, "0");
  const fractionUnits = BigInt(paddedFraction);

  return wholeUnits + fractionUnits;
};

export const toStellarAmount = (minor: bigint): string => {
  if (minor < 0n) throw new Error("Authoritative minor units cannot be negative");

  const quotient = minor / SCALE;
  const remainder = minor % SCALE;
  const remainderStr = remainder.toString().padStart(DECIMALS, "0");

  return `${quotient}.${remainderStr}`;
};

export const formatDisplay = (minor: bigint, code: string): string => {
  const stellar = toStellarAmount(minor);
  const [whole, fraction] = stellar.split(".");

  let trimmedFraction = fraction.replace(/0+$/, "");
  if (trimmedFraction.length < 2) {
    trimmedFraction = trimmedFraction.padEnd(2, "0");
  }

  return `${whole}.${trimmedFraction} ${code}`.trim();
};
