import { Component, EventEmitter, Input, Output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";

@Component({
  selector: "app-pagination",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./pagination.component.html",
  styleUrls: ["./pagination.component.css"]
})
export class PaginationComponent {
  @Input() page: number = 0;
  @Input() totalPages: number = 1;
  @Input() loading: boolean = false;
  @Input() totalElements?: number;
  @Input() pageSize?: number;
  @Input() pageSizes?: number[];
  @Input() sizeLabel: string = "por página";

  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();

  get hasPrevious(): boolean {
    return this.page > 0 && !this.loading;
  }

  get hasNext(): boolean {
    return (this.page + 1 < (this.totalPages || 1)) && !this.loading;
  }

  get displayTotalPages(): number {
    return Math.max(1, this.totalPages || 1);
  }

  onPrev(): void {
    if (this.hasPrevious) {
      this.pageChange.emit(this.page - 1);
    }
  }

  onNext(): void {
    if (this.hasNext) {
      this.pageChange.emit(this.page + 1);
    }
  }

  onSizeChange(newSize: number): void {
    this.pageSizeChange.emit(newSize);
  }
}

