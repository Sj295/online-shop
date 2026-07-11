interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
}

export function SectionHeader({ title, subtitle, action }: SectionHeaderProps) {
  return (
    <div className="flex items-end justify-between mb-8 md:mb-10">
      <div>
        <h2 className="text-xl md:text-2xl font-medium text-stone-900 tracking-tight">{title}</h2>
        {subtitle && <p className="mt-2 text-sm text-stone-500">{subtitle}</p>}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}
