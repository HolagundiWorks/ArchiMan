import React from 'react';
import { cn } from '../../lib/cn';

export interface ListRowProps {
  leading?: React.ReactNode;
  title: React.ReactNode;
  supporting?: React.ReactNode;
  meta?: React.ReactNode;
  trailing?: React.ReactNode;
  onClick?: () => void;
  className?: string;
  selected?: boolean;
}

/** Scannable enterprise list row — replaces hand-rolled card stacks for registers. */
export function ListRow({
  leading,
  title,
  supporting,
  meta,
  trailing,
  onClick,
  className,
  selected,
}: ListRowProps) {
  const Comp: any = onClick ? 'button' : 'div';
  return (
    <Comp
      type={onClick ? 'button' : undefined}
      onClick={onClick}
      className={cn(
        'w-full text-left flex items-start gap-3 px-4 py-3 transition',
        'border-b border-outline-variant/70 last:border-b-0',
        onClick && 'hover:bg-surface-container-low focus-visible:bg-surface-container-low',
        selected && 'bg-primary-container/35',
        className
      )}
    >
      {leading && <div className="shrink-0 mt-0.5 text-on-surface-variant">{leading}</div>}
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <div className="type-title-sm truncate">{title}</div>
          {meta && <div className="shrink-0 type-label-sm normal-case tracking-normal">{meta}</div>}
        </div>
        {supporting && <div className="type-body-sm mt-0.5 line-clamp-2">{supporting}</div>}
      </div>
      {trailing && <div className="shrink-0 flex items-center gap-1">{trailing}</div>}
    </Comp>
  );
}
