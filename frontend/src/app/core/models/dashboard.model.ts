export interface DashboardSlide {
    id: string;
    title: string;
    subtitle: string;
    value: string;
    tone: "warning" | "info" | "ok";
    imageUrl?: string;
    category?: string;
    badge?: string;
}
