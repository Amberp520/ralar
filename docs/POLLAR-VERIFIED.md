# POLLAR SDK & SERVER INTEGRATION SPECIFICATION (VERIFIED)

## 1. Executive Summary
This document records the verified API surface and architectural boundaries for the **Pollar SDK** and **Pollar Server Infrastructure** integrated into **Ralar**.

- Package: `@pollar/core` (version 0.11.x) & `@pollar/react`
- Primary server endpoint: `https://server.api.pollar.xyz`
- Settlement network: Stellar Testnet
- Native asset: XLM (`{ type: "native" }`)
- Regulated asset: USDC (`{ type: "credit_alphanum4", code: "USDC", issuer: "G..." }`)

---

## 2. Security & Authority Boundaries (Non-Negotiable)

1. **Client is a Signing Device Only**:
   - The user's wallet authorizes/signs payments through the authenticated session.
   - The browser or mobile client has **ZERO financial authority**.
   - The client MUST NOT determine amounts, recipients, destinations, memos, or payment statuses.

2. **Server is the Authoritative Financial Authority**:
   - Server derives and validates all monetary values in BigInt minor units (10^7 decimals).
   - Server generates unique cryptographic memos (`RLR-XXXXXXXX`).
   - Server provides the exact transaction parameters to the client for signature.
   - Server independently verifies on-chain transaction outcomes against Stellar RPC / Horizon.
   - Server only transitions records to `PAID` after independent confirmation.

3. **Key Isolation**:
   - `POLLAR_SECRET_KEY` (`sec_testnet_...`): Stored strictly server-side / backend environment. NEVER exposed to client bundles or APK assets.
   - `NEXT_PUBLIC_POLLAR_PUBLISHABLE_KEY` (`pub_testnet_...`): Used for client authentication and wallet connection.

---

## 3. Verified Client SDK Surface (`usePollar()`)

```typescript
interface PollarWallet {
  address: string; // Stellar public key 'G...'
  custody: 'internal' | 'smart' | 'external';
  provider: string;
  existsOnStellar: boolean;
}

interface UsePollarReturn {
  isAuthenticated: boolean;
  wallet: PollarWallet | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  openLoginModal: () => void;
  runTx: (
    type: 'payment',
    payload: {
      destination: string;
      amount: string; // e.g. "25.0000000"
      asset: {
        type: 'credit_alphanum4' | 'credit_alphanum12' | 'native';
        code?: string;
        issuer?: string;
      };
    },
    options?: {
      memo?: {
        type: 'text' | 'id' | 'hash';
        value: string; // e.g. "RLR-3F9K2Q8M"
      };
    }
  ) => Promise<{
    status: 'success' | 'pending' | 'error';
    hash?: string;
    code?: string;
    message?: string;
  }>;
  walletBalance: string;
  refreshWalletBalance: () => Promise<void>;
}
```

---

## 4. Verified Server API Endpoints

### Base URL
`https://server.api.pollar.xyz`

### Authentication Header
`x-pollar-api-key: sec_testnet_...`

### Wallet Funding
`POST /v1/wallets/fund`
- Request body:
  ```json
  {
    "address": "G..."
  }
  ```
- Success response:
  ```json
  {
    "code": 200,
    "success": true,
    "content": {
      "funded": true,
      "transactionHash": "..."
    }
  }
  ```
- 409 Conflict: Wallet already funded (treated as idempotent success).

---

## 5. Stellar On-Chain Verification Protocol

Verification algorithm implemented in Ralar:
1. Verify transaction exists on Stellar testnet (RPC `getTransaction` or Horizon `/transactions/{hash}`).
2. Verify transaction status is `SUCCESS` / `successful`.
3. Locate payment operation inside transaction operations.
4. If transaction is wrapped in a Fee-Bump envelope, traverse into the inner transaction envelope.
5. Verify `destination` matches the authoritative organizer or winner wallet address.
6. Verify `asset_code` matches `USDC` (or configured asset) and `asset_issuer` matches authoritative issuer.
7. Verify `amount` matches authoritative amount converted to 7-decimal minor units (`BigInt`).
8. Verify transaction memo matches server-generated `memoRef` (`RLR-XXXXXXXX`).
9. Return `{ ok: true, hash, confirmedAt }` or `{ ok: false, reason, detail }`.
