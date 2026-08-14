interface StatBarProps {
  name: string;
  baseStat: number;
}

export function StatBar({ name, baseStat }: StatBarProps) {
  const width = Math.min(100, baseStat);

  return (
    <div>
      <div className="mb-1 flex justify-between text-sm capitalize text-slate-700">
        <span>{name}</span>
        <span>{baseStat}</span>
      </div>
      <div className="h-2 rounded-full bg-slate-200">
        <div
          className="h-2 rounded-full bg-poke-blue"
          style={{ width: `${width}%` }}
          aria-hidden="true"
        />
      </div>
    </div>
  );
}
