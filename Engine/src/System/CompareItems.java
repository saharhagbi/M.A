package System;

import Objects.Item;

import java.util.Comparator;

public class CompareItems implements Comparator<Item>
{
    public int compare(Item i_FirstItem, Item i_SecondItem)
    {
        StringBuilder strFirst;
        StringBuilder strSecond;

        strFirst = buildStringForItem(i_FirstItem);
        strSecond = buildStringForItem(i_SecondItem);

        return strFirst.toString().compareTo(strSecond.toString());
    }

    private StringBuilder buildStringForItem(Item i_FirstItem)
    {
        StringBuilder strForBuilding = new StringBuilder();

        strForBuilding.append(i_FirstItem.getName());
        strForBuilding.append(i_FirstItem.getSHA1());
        strForBuilding.append(i_FirstItem.getTypeOfFile().toString());

        return strForBuilding;
    }
}
