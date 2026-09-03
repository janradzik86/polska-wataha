import { createFileRoute } from "@tanstack/react-router";
import { Guard } from "@/components/guard";
import { MeshLab } from "@/components/mesh-lab";

export const Route = createFileRoute("/mesh")({ component: Page });

function Page() {
  return (
    <Guard>
      <MeshLab />
    </Guard>
  );
}
