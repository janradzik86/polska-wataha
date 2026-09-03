import { useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { GROK_PROVIDERS, authClient, authEnabled, signIn } from "@/lib/auth/client";
import { DEMO_EMAIL, DEMO_PASSWORD } from "@/lib/siatka";
import { ensureDemoUser } from "@/lib/server/siatka";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { BrandLockup, FlagStripe, Kotwica, OrzelBialy, PatriotPanel } from "@/components/brand";

export const Route = createFileRoute("/login")({ component: Login });

function Login() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<"in" | "up">("in");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function afterAuth() {
    navigate({ to: "/" });
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === "up") {
        const res = await authClient.signUp.email({ email, password, name: name || email.split("@")[0] });
        if (res.error) throw new Error(res.error.message || "Nie udało się utworzyć konta");
      } else {
        const res = await authClient.signIn.email({ email, password });
        if (res.error) throw new Error(res.error.message || "Logowanie nieudane");
      }
      await afterAuth();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Błąd");
    } finally {
      setBusy(false);
    }
  }

  async function demo() {
    setBusy(true);
    setError(null);
    try {
      await ensureDemoUser();
      const res = await authClient.signIn.email({ email: DEMO_EMAIL, password: DEMO_PASSWORD });
      if (res.error) {
        const up = await authClient.signUp.email({
          email: DEMO_EMAIL,
          password: DEMO_PASSWORD,
          name: "Anna Kowalska",
        });
        if (up.error) throw new Error(up.error.message || res.error.message || "Demo niedostępne");
      }
      await afterAuth();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Błąd demo");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="min-h-dvh bg-bg text-fg">
      <FlagStripe />
      <div className="grid min-h-[calc(100dvh-10px)] lg:grid-cols-2">
        <PatriotPanel className="hidden min-h-[28rem] lg:flex" />

        <div className="relative flex flex-col justify-center px-6 py-10 sm:px-12 patriot-wash">
          <div className="mx-auto w-full max-w-md">
            <div className="mb-8 lg:hidden">
              <div className="mb-4 flex items-center gap-3 text-primary">
                <OrzelBialy className="h-9 w-8" />
                <Kotwica className="h-8 w-6" />
              </div>
              <BrandLockup size="lg" stacked />
            </div>
            <p className="hidden items-center gap-2 text-xs uppercase tracking-[0.22em] text-muted lg:flex">
              <OrzelBialy className="h-4 w-3.5 text-primary" />
              V0.2 · Warszawa · Biało-czerwoni
            </p>
            <div className="mt-3 hidden lg:block">
              <BrandLockup size="lg" stacked />
            </div>
            <p className="mt-5 max-w-md text-base text-muted">
              Sąsiedzka sieć ogłoszeń, wymiany, pomocy i komunikacji kryzysowej. Wataha trzyma się razem —
              offline, w kryzysie i bez centrali.
            </p>

            <div className="mt-8 rounded-2xl border border-border bg-surface p-6">
              <div className="flag-stripe mb-5 rounded-full" />
              <h2 className="font-display text-2xl font-medium uppercase tracking-wide">
                {mode === "in" ? "Wejdź do watahy" : "Utwórz konto testowe"}
              </h2>
              <p className="mt-1 text-sm text-muted">Email i hasło albo konto demonstracyjne.</p>

              {authEnabled ? (
                <form className="mt-6 space-y-3" onSubmit={onSubmit}>
                  {mode === "up" ? (
                    <div className="space-y-1">
                      <Label htmlFor="name">Imię</Label>
                      <Input id="name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Anna Kowalska" />
                    </div>
                  ) : null}
                  <div className="space-y-1">
                    <Label htmlFor="email">Email</Label>
                    <Input
                      id="email"
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="ty@wataha.pl"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="password">Hasło</Label>
                    <Input
                      id="password"
                      type="password"
                      required
                      minLength={8}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </div>
                  {error ? <p className="text-sm text-danger">{error}</p> : null}
                  <Button type="submit" className="w-full" disabled={busy}>
                    {mode === "in" ? "Zaloguj" : "Utwórz konto"}
                  </Button>
                  <Button type="button" variant="secondary" className="w-full" disabled={busy} onClick={demo}>
                    Wejdź na konto demo
                  </Button>
                  <p className="text-center text-[11px] text-subtle">
                    {DEMO_EMAIL} · {DEMO_PASSWORD}
                  </p>
                  <button
                    type="button"
                    className="w-full text-center text-sm text-muted underline-offset-2 hover:underline"
                    onClick={() => setMode(mode === "in" ? "up" : "in")}
                  >
                    {mode === "in" ? "Nie masz konta? Załóż testowe" : "Masz konto? Zaloguj się"}
                  </button>
                  <div className="space-y-2 pt-2">
                    {GROK_PROVIDERS.map((p) => (
                      <Button
                        key={p.providerId}
                        type="button"
                        variant="outline"
                        className="w-full"
                        onClick={() => signIn(p.providerId, { callbackURL: "/" })}
                      >
                        Kontynuuj z {p.label}
                      </Button>
                    ))}
                  </div>
                </form>
              ) : (
                <p className="mt-4 text-sm text-muted">Logowanie jest wyłączone.</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
