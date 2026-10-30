import React, { useEffect } from 'react';
import { X } from 'lucide-react';
import { cn } from '../../lib/cn';
import { Button } from './Button';

export interface DialogProps {
  open: boolean;
  title: string;
  description?: string;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClass = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
};

/** Modal dialog with scrim — replaces ad-hoc fixed overlays. */
export function Dialog({
  open,
  title,
  description,
  onClose,
  children,
  footer,
  size = 'md',
  className,
}: DialogProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="dialog-title"
    >
      <button
        type="button"
        aria-label="Dismiss dialog"
        className="absolute inset-0 bg-scrim/40 backdrop-blur-[1px]"
        onClick={onClose}
      />
      <div
        className={cn(
          'relative z-10 w-full bg-surface-container-lowest text-on-surface',
          'rounded-t-xl sm:rounded-xl elevation-3 border border-outline-variant',
          'max-h-[92vh] flex flex-col',
          sizeClass[size],
          className
        )}
      >
        <header className="flex items-start justify-between gap-3 px-5 pt-5 pb-3 border-b border-outline-variant/60">
          <div className="min-w-0">
            <h2 id="dialog-title" className="type-title-md">
              {title}
            </h2>
            {description && <p className="type-body-sm mt-1">{description}</p>}
          </div>
          <Button variant="text" size="sm" aria-label="Close" onClick={onClose} className="!px-2 shrink-0">
            <X className="w-4 h-4" />
          </Button>
        </header>
        <div className="px-5 py-4 overflow-y-auto flex-1">{children}</div>
        {footer && (
          <footer className="px-5 py-4 border-t border-outline-variant/60 flex flex-wrap justify-end gap-2 bg-surface-container-low/50">
            {footer}
          </footer>
        )}
      </div>
    </div>
  );
}
