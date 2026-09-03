export const APP_NAME = "Polska Wataha";
export const APP_TAGLINE = "Czarne Wilki Prawdy";
export const DEMO_EMAIL = "demo@siatka.app";
export const DEMO_PASSWORD = "siatka-demo-2026";

export const WARSAW = {
  lat: 52.2297,
  lng: 21.0122,
  north: 52.33,
  south: 52.14,
  west: 20.84,
  east: 21.2,
};

export type ListingKind = "offer" | "want" | "giveaway" | "exchange";
export type HelpKind = "need" | "offer";
export type Urgency = "low" | "normal" | "high" | "crisis";
export type NodeKind = "phone" | "lora" | "wifi" | "bt";
export type NodeStatus = "online" | "degraded" | "down";
export type AdapterId = "internet" | "bluetooth" | "wifi_direct" | "lora";

export type Profile = {
  id: string;
  userId: string | null;
  displayName: string;
  bio: string;
  district: string;
  lat: number | null;
  lng: number | null;
  reputation: number;
  avatarHue: number;
  isSeed: boolean;
  createdAt: string;
  badges: Badge[];
};

export type Badge = {
  id: string;
  slug: string;
  title: string;
  description: string;
  earnedAt?: string;
};

export type Listing = {
  id: string;
  authorId: string;
  authorName: string;
  authorHue: number;
  kind: ListingKind;
  title: string;
  body: string;
  category: string;
  lat: number | null;
  lng: number | null;
  district: string;
  status: string;
  createdAt: string;
};

export type HelpPost = {
  id: string;
  authorId: string;
  authorName: string;
  authorHue: number;
  kind: HelpKind;
  title: string;
  body: string;
  urgency: Urgency;
  lat: number | null;
  lng: number | null;
  district: string;
  status: string;
  createdAt: string;
};

export type ThreadSummary = {
  id: string;
  title: string;
  listingId: string | null;
  lastBody: string;
  lastAt: string;
  peerName: string;
  peerHue: number;
};

export type ChatMessage = {
  id: string;
  threadId: string;
  senderId: string;
  senderName: string;
  body: string;
  createdAt: string;
};

export type ExchangeOffer = {
  id: string;
  listingId: string;
  listingTitle: string;
  fromId: string;
  fromName: string;
  toId: string;
  offerText: string;
  status: string;
  createdAt: string;
};

export type CrisisAlert = {
  id: string;
  authorId: string;
  authorName: string;
  kind: string;
  body: string;
  lat: number | null;
  lng: number | null;
  createdAt: string;
};

export type MeshNode = {
  id: string;
  label: string;
  kind: NodeKind;
  lat: number;
  lng: number;
  status: NodeStatus;
  lastSeen: string;
};

export type HelpPoint = {
  id: string;
  title: string;
  kind: string;
  lat: number;
  lng: number;
  note: string;
};

export type MapPin = {
  id: string;
  lat: number;
  lng: number;
  label: string;
  tone: "listing" | "help" | "need" | "node" | "point" | "me" | "crisis";
  href?: string;
};

export const KIND_LABEL: Record<ListingKind, string> = {
  offer: "Oferuję",
  want: "Szukam",
  giveaway: "Oddam",
  exchange: "Wymiana",
};

export const HELP_LABEL: Record<HelpKind, string> = {
  need: "Potrzebuję pomocy",
  offer: "Mogę pomóc",
};

export const CATEGORIES = [
  "Narzędzia",
  "Dom",
  "Jedzenie",
  "Transport",
  "Dzieci",
  "Ogród",
  "Elektronika",
  "Odzież",
  "Inne",
] as const;

export function project(lat: number, lng: number) {
  const x = ((lng - WARSAW.west) / (WARSAW.east - WARSAW.west)) * 100;
  const y = ((WARSAW.north - lat) / (WARSAW.north - WARSAW.south)) * 100;
  return { x: Math.min(98, Math.max(2, x)), y: Math.min(98, Math.max(2, y)) };
}
