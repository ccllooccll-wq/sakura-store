import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { NavbarComponent } from "../navbar/navbar.component";

@Component({
  selector: "app-shell",
  standalone: true,
  imports: [NavbarComponent, RouterOutlet],
  templateUrl: "./app-shell.component.html",
})
export class AppShellComponent {}
