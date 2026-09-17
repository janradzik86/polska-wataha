import { Link, useRouterState } from "@tanstack/react-router";
import {
  Bell,
  HandHelping,
  LayoutGrid,
  Map as MapIcon,
  MessageCircle,
  Radio,
  Siren,
  User,
} from "lucide-react";
import { SignedIn, UserButton } from "@/lib/auth/gates";
import { useCurrentUserState } from "@/lib/auth/use-current-user";
import { useSiatka } from "@/lib/store";
import { ConnectionBar } from "@/components/connection-bar";
import { BrandLockup, FlagStripe, Kotwica } from "@/components/brand";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const NAV = [
  { to: "/", label: "Ogłoszenia", icon: LayoutGrid },
  { to: "/map", label: "Mapa", icon: MapIcon },
  { to: "/help", label: "Pomoc", icon: HandHelping },
  { to: "/messages", label: "Wiadomości", icon: MessageCircle },
  { to: "/profile", label: "Profil", icon: User },
] as const;

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const crisis = useSiatka((s) => s.crisis);
  const setCrisis = useSiatka((s) => s.setCrisis);
  const { isPending } = useCurrentUserState();

  return (
    <div className={cn("min-h-dvh bg-bg text-fg patriot-wash", crisis && "crisis-root")}>
      <FlagStripe />
      <div className="mx-auto flex min-h-dvh max-w-6xl">
        <aside className="sticky top-0 hidden h-dvh w-64 shrink-0 flex-col border-r border-border p-4 md:flex">
          <Link to="/" className="block">
            <BrandLockup size="sm" />
          </Link>
          <nav className="mt-6 flex flex-1 flex-col gap-1">
            {NAV.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "flex h-11 items-center gap-3 rounded-md px-3 text-sm",
                  pathname === item.to ? "bg-primary text-primary-fg" : "text-muted hover:bg-elevated hover:text-fg",
                )}
              >
                <item.icon className="size-4" />
                {item.label}
              </Link>
            ))}
            <Link
              to="/lora"
              className={cn(
                "flex h-11 items-center gap-3 rounded-md px-3 text-sm",
                pathname === "/lora" ? "bg-primary text-primary-fg" : "text-muted hover:bg-elevated hover:text-fg",
              )}
            >
              <Radio className="size-4" />
              Heltec V4
            </Link>
            <Link
              to="/status"
              className={cn(
                "flex h-11 items-center gap-3 rounded-md px-3 text-sm",
                pathname === "/status" ? "bg-primary text-primary-fg" : "text-muted hover:bg-elevated hover:text-fg",
              )}
            >
              <Bell className="size-4" />
              Status
            </Link>
          </nav>
          <Button variant="crisis" className="w-full" onClick={() => setCrisis(true)}>
            <Siren className="size-4" />
            Kryzys
          </Button>
          <div className="mt-3 flex items-center justify-between">
            {isPending ? <div className="h-8 w-8 animate-pulse rounded-full bg-elevated" /> : <UserButton />}
            <Kotwica className="h-5 w-4 text-primary" />
          </div>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <ConnectionBar />
          <header className="flex items-center justify-between border-b border-border px-4 py-3 md:hidden">
            <Link to="/" className="block">
              <BrandLockup size="sm" compact />
            </Link>
            <div className="flex items-center gap-2">
              <Button size="sm" variant="crisis" onClick={() => setCrisis(true)}>
                Kryzys
              </Button>
              <SignedIn>
                {isPending ? <div className="h-8 w-8 animate-pulse rounded-full bg-elevated" /> : <UserButton />}
              </SignedIn>
            </div>
          </header>
          <main className="flex-1 px-4 py-5 pb-24 md:px-8 md:pb-8">{children}</main>
          <nav className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-5 border-t border-border bg-bg/95 backdrop-blur md:hidden">
            {NAV.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "flex h-16 flex-col items-center justify-center gap-1 text-[10px] uppercase tracking-wider",
                  pathname === item.to ? "text-primary" : "text-muted",
                )}
              >
                <item.icon className="size-5" />
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
      </div>
    </div>
  );
}
