// ngafid-frontend/src/app/pages/protected/flights/types_events.ts

export interface EventSelectionState {
    universalEvents: Set<string>;
    perFlightEvents: Record<number, Set<string>>;
}