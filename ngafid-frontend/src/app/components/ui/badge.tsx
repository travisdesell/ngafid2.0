import { cva, type VariantProps } from "class-variance-authority";
import * as React from "react";

import { cn } from "@/lib/utils";
import { buttonVariants } from "./button";

const badgeVariants = cva(
  "inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-primary text-primary-foreground shadow hover:bg-primary/80",
        secondary:
          "border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",
        destructive:
          "border-transparent bg-destructive text-destructive-foreground shadow hover:bg-destructive/80",
        outline: "text-foreground",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

export type BadgeVariant = VariantProps<typeof badgeVariants>["variant"]

type ClickableBadgeProps = React.ComponentPropsWithoutRef<"button"> &
  VariantProps<typeof badgeVariants>

type NonClickableBadgeProps = React.ComponentPropsWithoutRef<"div"> &
  VariantProps<typeof badgeVariants> & {
    onClick?: never
  }

export type BadgeProps = ClickableBadgeProps | NonClickableBadgeProps

function isClickableBadgeProps(props: BadgeProps): props is ClickableBadgeProps {
  return "onClick" in props && typeof props.onClick === "function"
}

function Badge(props: BadgeProps) {
  // Has onClick: Return button
  if (isClickableBadgeProps(props)) {
    const { className, variant, ...buttonProps } = props
    return (
      <button
        type="button"
        className={cn(
          badgeVariants({ variant }),
          buttonVariants({ variant: "ghost", size: "sm" }),
          className
        )}
        {...buttonProps}
      />
    )
  }

  // Normal: Return div
  const { className, variant, ...divProps } = props
  return <div className={cn(badgeVariants({ variant }), className)} {...divProps} />
}

export { Badge, badgeVariants };

