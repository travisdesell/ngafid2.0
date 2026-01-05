// ngafid-frontend/src/app/components/modals/suspense_loading.tsx
import { Loader2 } from "lucide-react";

export default function SuspenseLoading() {

    return <Loader2 className="animate-spin h-6 w-6 text-foreground absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />;

}