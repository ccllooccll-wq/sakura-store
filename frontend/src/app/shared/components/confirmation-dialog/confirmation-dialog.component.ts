import { AfterViewInit, Component, ElementRef, Input, Output, EventEmitter, ViewChild } from "@angular/core";

@Component({
  selector: "app-confirmation-dialog",
  standalone: true,
  templateUrl: "./confirmation-dialog.component.html",
  styleUrls: ["./confirmation-dialog.component.css"],
})
export class ConfirmationDialogComponent implements AfterViewInit {
  @Input({ required: true }) title = "";
  @Input({ required: true }) message = "";
  @Input() confirmLabel = "Confirmar";
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
  @ViewChild("dialog", { static: true }) private dialog!: ElementRef<HTMLDialogElement>;

  ngAfterViewInit(): void {
    this.dialog.nativeElement.showModal();
  }

  cancel(event: Event): void {
    event.preventDefault();
    this.cancelled.emit();
  }
}
