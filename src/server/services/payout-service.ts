/**
 * =========================================================================================
 * RALAR SECURITY ARCHITECTURE & FINANCIAL SETTLEMENT REASONING
 * =========================================================================================
 * 1. THE BROWSER / CLIENT IS THE SIGNING DEVICE, NEVER THE FINANCIAL AUTHORITY.
 *    The authenticated Pollar SDK session inside the client is strictly used to sign transactions
 *    using the user's custody key. The client has zero authority over financial values.
 *
 * 2. AUTHORITATIVE VALUES ORIGINATE EXCLUSIVELY FROM THE SERVER:
 *    - Destination wallet address is resolved server-side from the database.
 *    - Payout amount is calculated server-side in BigInt minor units (10^7 decimals).
 *    - Recipient user ID and eligibility are strictly validated server-side.
 *    - Memo reference (RLR-XXXXXXXX) is generated server-side and uniquely persisted.
 *
 * 3. INDEPENDENT ON-CHAIN VERIFICATION:
 *    The server never accepts a client's claim of "success". The client only reports
 *    a transaction hash. The server independently queries Stellar testnet (RPC / Horizon)
 *    to verify:
 *      a. Transaction exists and has status = SUCCESS.
 *      b. Payment operation matches destination, asset code, and asset issuer.
 *      c. Payment operation amount matches the authoritative minor units exactly.
 *      d. The on-chain memo matches the server-generated memoRef.
 *
 * 4. PAID STATUS IS ONLY WRITTEN AFTER INDEPENDENT VERIFICATION:
 *    Under no circumstances is a Payment or Payout marked PAID until verifiedAt is set
 *    by the independent on-chain verification engine.
 * =========================================================================================
 */

export interface PayoutAuthorization {
  payoutId: string;
  destination: string;
  amount: string; // e.g. "100.0000000"
  assetCode: string;
  assetIssuer: string | null;
  memoRef: string;
}
