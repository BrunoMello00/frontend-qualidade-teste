import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { RelatoriosComponent } from './relatorios.component';

@NgModule({
  declarations: [
    RelatoriosComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild([
      { path: '', component: RelatoriosComponent }
    ])
  ]
})
export class RelatoriosModule { }