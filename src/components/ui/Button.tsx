import React from 'react';
import { cn } from '../../lib/cn';

type Variant = 'filled' | 'tonal' | 'outlined' | 'text' | 'destructive';
type Size = 'sm' | 'md' | 'lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  leadingIcon?: React.ReactNode;
  trailingIcon?: React.ReactNode;
}

const variantClass: Record<Variant, string> = {
  filled:
    'bg-primary text-on-primary hover:brightness-95 active:brightness-90 elevation-1 border border-transparent',
  tonal:
    'bg-primary-container text-on-primary-container hover:brightness-95 border border-transparent',
  outlined:
    'bg-transparent text-primary border border-outline-variant hover:bg-primary-container/40',
  text: 'bg-transparent text-primary border border-transparent hover:bg-primary-container/40',
  destructive:
    'bg-error text-on-error hover:brightness-95 border border-transparent elevation-1',
};

const sizeClass: Record<Size, string> = {
  sm: 'h-9 px-3 text-xs gap-1.5 rounded-sm',
  md: 'h-10 px-4 text-sm gap-2 rounded-md',
  lg: 'h-12 px-5 text-sm gap-2 rounded-md',
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'filled',
      size = 'md',
      leadingIcon,
      trailingIcon,
      children,
      type = 'button',
      ...props
    },
    ref
  ) => (
    <button
      ref={ref}
      type={type}
      className={cn(
        'inline-flex items-center justify-center font-semibold transition select-none',
        'disabled:opacity-40 disabled:pointer-events-none',
        'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
        variantClass[variant],
        sizeClass[size],
        className
      )}
      {...props}
    >
      {leadingIcon}
      {children}
      {trailingIcon}
    </button>
  )
);
Button.displayName = 'Button';
