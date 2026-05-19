export function Skeleton({ className = 'h-4 w-full' }) {
  return <div className={`animate-pulse rounded bg-accent-line/70 dark:bg-ink-700/70 ${className}`} />;
}

export function SkeletonCard({ rows = 4 }) {
  return (
    <div className="card p-6 space-y-3">
      <Skeleton className="h-5 w-1/3" />
      <Skeleton className="h-4 w-2/3" />
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-4 w-full" />
      ))}
    </div>
  );
}
