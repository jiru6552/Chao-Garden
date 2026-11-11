package com.chaogarden.genes;

import com.chaogarden.item.CareItem;

/** A sealed sample considered in the Chao's hands before the server commits its benefits. */
public final class GeneItem extends CareItem {
  public GeneItem(String animal, int index) {
    super(Kind.ANIMAL, animal, index);
  }
}
