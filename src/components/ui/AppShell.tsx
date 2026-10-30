import React from 'react';
import { cn } from '../../lib/cn';

export function AppShell({
  header,
  nav,
  children,
  className,
}: {
  header: React.ReactNode;
  nav?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn('min-h-screen bg-surface flex flex-col text-on-surface', className)}>
      <div className="sticky top-0 z-30 bg-sidebar/95 backdrop-blur border-b border-outline-variant">
        {header}
        {nav}
      </div>
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-5">
        {children}
      </main>
    </div>
  );
}

export function ShellHeader({
  breadcrumb,
  leading,
  title,
  subtitle,
  actions,
}: {
  breadcrumb?: React.ReactNode;
  leading?: React.ReactNode;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  actions?: React.ReactNode;
}) {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      {breadcrumb && (
        <div className="flex items-center gap-2 py-2 type-label-sm border-b border-outline-variant/50">
          {breadcrumb}
        </div>
      )}
      <div className="flex items-center justify-between gap-3 py-3">
        <div className="flex items-center gap-3 min-w-0">
          {leading}
          <div className="min-w-0">
            <div className="type-title-md truncate">{title}</div>
            {subtitle && <div className="type-body-sm truncate">{subtitle}</div>}
          </div>
        </div>
        {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
      </div>
    </div>
  );
}

export function BottomNav({
  items,
  value,
  onChange,
}: {
  items: { id: string; label: string; icon: React.ReactNode }[];
  value: string;
  onChange: (id: string) => void;
}) {
  return (
    <nav className="sticky bottom-0 z-30 border-t border-outline-variant bg-surface-container-lowest/95 backdrop-blur">
      <div className="max-w-7xl mx-auto px-2 sm:px-4 flex">
        {items.map((item) => {
          const active = item.id === value;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onChange(item.id)}
              className={cn(
                'flex-1 flex flex-col items-center justify-center gap-1 py-2.5 text-[11px] font-semibold transition',
                active ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'
              )}
            >
              <span
                className={cn(
                  'inline-flex items-center justify-center w-12 h-7 rounded-sm transition',
                  active && 'bg-primary-container text-on-primary-container'
                )}
              >
                {item.icon}
              </span>
              {item.label}
            </button>
          );
        })}
      </div>
    </nav>
  );
}

export function Fab({
  label,
  icon,
  onClick,
  className,
}: {
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
  className?: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className={cn(
        'fixed bottom-20 right-4 sm:bottom-6 sm:right-6 z-40',
        'inline-flex items-center gap-2 h-10 px-4 rounded-sm',
        'bg-primary-container text-on-primary-container font-semibold elevation-3 text-[12px]',
        'hover:brightness-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
        className
      )}
    >
      {icon}
      <span className="hidden sm:inline">{label}</span>
    </button>
  );
}
