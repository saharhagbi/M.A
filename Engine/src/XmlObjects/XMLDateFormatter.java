package XmlObjects;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XMLDateFormatter
{
    private static DateFormat m_FormatterToDate = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

    public static Date FormatStringToDateType(String i_DateOfCreation) throws ParseException
    {
        Date dateWanted = null;

        try
        {
            dateWanted = m_FormatterToDate.parse(i_DateOfCreation);
        } catch (Exception xmlException)
        {
            throw xmlException;
        }

        return dateWanted;
    }
}
