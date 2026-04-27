import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-paginator',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (pg.pageCount() > 1 || pg.range() !== '0 results') {
      <div class="fs-pagination">
        <span class="fs-pagination-info">{{ pg.range() }}</span>
        <div class="fs-pagination-btns">
          <button class="fs-pg-btn" (click)="pg.first()" [disabled]="pg.page()===1"><i class="bi bi-chevron-double-left"></i></button>
          <button class="fs-pg-btn" (click)="pg.prev()"  [disabled]="pg.page()===1"><i class="bi bi-chevron-left"></i></button>
          <span class="fs-pg-num">{{ pg.page() }} / {{ pg.pageCount() }}</span>
          <button class="fs-pg-btn" (click)="pg.next()" [disabled]="pg.page()===pg.pageCount()"><i class="bi bi-chevron-right"></i></button>
          <button class="fs-pg-btn" (click)="pg.last()" [disabled]="pg.page()===pg.pageCount()"><i class="bi bi-chevron-double-right"></i></button>
        </div>
      </div>
    }
  `
})
export class PaginatorComponent {
  @Input() pg!: any;
}
