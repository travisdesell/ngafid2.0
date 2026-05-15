// ngafid-frontend/src/app/components/ui/checkbox-static.tsx
import { cn } from "@/lib/utils";
import { Check } from "lucide-react";

interface CheckboxStaticProps {
    checked: boolean
    className?: string
}

/*
    Used when the 'checked' state depends on some
    external condition.

    This component itself is non-interactive.
*/

export function CheckboxStatic({ checked, className }: CheckboxStaticProps) {

    return (
        <span
            aria-hidden="true"
            className={cn(
                "hover:bg-accent group-hover:bg-accent transition-[background-color]",
                "h-5 w-5 shrink-0 rounded-sm border border-primary shadow",
                "flex items-center justify-center",
                checked && "bg-primary text-primary-foreground",
                className,
            )}
        >
            {
                (checked)
                &&
                <Check className="h-3 w-4" />
            }
        </span>
    );

}