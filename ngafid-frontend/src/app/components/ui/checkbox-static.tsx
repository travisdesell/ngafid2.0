// ngafid-frontend/src/app/components/ui/checkbox-static.tsx
import { cn } from "@/lib/utils"
import { Check } from "lucide-react"

type CheckboxStaticProps = {
    checked: boolean
    className?: string
}

export function CheckboxStatic({ checked, className }: CheckboxStaticProps) {

    return (
        <span
            aria-hidden="true"
            className={cn(
                "h-4 w-4 shrink-0 rounded-sm border border-primary shadow",
                "flex items-center justify-center",
                checked && "bg-primary text-primary-foreground",
                className,
            )}
        >
            {
                (checked)
                &&
                <Check className="h-4 w-4" />
            }
        </span>
    )

}