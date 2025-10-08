// src/constants/access.ts
export const ACCESS_TYPES = [
  "DENIED",
  "WAITING",
  "VIEW",
  "UPLOAD",
  "MANAGER",
] as const;

export type AccessType = (typeof ACCESS_TYPES)[number];