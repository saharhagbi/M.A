package Objects;

import System.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Item
{
    private final TypeOfFile m_TypeOfFile;
    private Path m_Path;
    private String m_SHA1;
    private User m_UserName;
    private Date m_DateOfCreation;
    private String m_ItemName;

    public Item(Path i_Path, String i_SHA1, TypeOfFile i_TypeOfFile, User i_UserName, Date i_DateOfCreation, String i_BlobName)
    {
        this.m_ItemName = i_BlobName;
        this.m_Path = i_Path;
        this.m_SHA1 = i_SHA1;
        this.m_TypeOfFile = i_TypeOfFile;
        this.m_UserName = i_UserName;
        this.m_DateOfCreation = i_DateOfCreation;
    }


    public static String[] GetItemsDetails(String Line)
    {
        String[] allFields = new String[5];//example: 123,50087888a7c34344416ec0fd600f394dadf3d9d8,FOLDER,Administrator,06.39.2019-06:39:27:027
        String[] lineOfDetails = Line.split(",");
        int i = 0;
        for (String ss : lineOfDetails)
        {
            allFields[i] = ss;
            i++;
        }
        return allFields;
    }

    public static boolean IsAFile(String lineOfDetails)
    {
        String[] itemDetails = Item.GetItemsDetails(lineOfDetails);
        if (itemDetails[2].equals("BLOB"))
            return true;
        else
            return false;
    }


    public static Date ParseDateWithFormat(String i_DateAsString) throws ParseException
    {
        DateFormat m_FormatterToDate = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");
        return m_FormatterToDate.parse(i_DateAsString);

    }

    public static String getDateStringByFormat(Date i_date)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");
        return dateFormat.format(i_date);
    }

    public static Path UnzipFile(Path i_From, Path i_DestinationFolder) throws IOException
    {
        File newFile = null;
        String fileZip = i_From.toString();
        File destDir = new File(i_DestinationFolder.toString());
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null)
        {
            newFile = newFile(destDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0)
            {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return newFile.toPath();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException
    {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator))
        {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public String getName()
    {
        return m_ItemName;
    }

    public User getUser()
    {
        return m_UserName;
    }

    public String getSHA1()
    {
        return m_SHA1;
    }

    public void setSHA1(String m_SHA1)
    {
        this.m_SHA1 = m_SHA1;
    }

    public TypeOfFile getTypeOfFile()
    {
        return m_TypeOfFile;
    }

    public Date getDate()
    {
        return m_DateOfCreation;
    }

    public Path GetPath()
    {
        return m_Path;
    }

    public enum TypeOfFile
    {
        BLOB, FOLDER
    }
}
