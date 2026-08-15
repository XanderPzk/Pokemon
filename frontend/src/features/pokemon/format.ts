export function formatMassKg(massHectograms: number): string {
  return `${(massHectograms / 10).toFixed(1)} kg`;
}
