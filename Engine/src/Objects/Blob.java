package Objects;

import System.User;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class Blob extends Item
{
    private String m_Content;


    public Blob(Path i_Path, String i_SHA1, String i_Content, TypeOfFile i_TypeOfFile, User i_UserName,
                Date i_DateOfCreation, String i_BlobName)
    {
        super(i_Path, i_SHA1, i_TypeOfFile, i_UserName, i_DateOfCreation, i_BlobName);
        this.m_Content = i_Content;
    }


    public static String getFileContent(File i_File) throws Exception
    {
        return ReadLineByLine(i_File);
    }

    public static String ReadLineByLine(File i_file) throws Exception
    {
        try
        {
            String content = new String(Files.readAllBytes(Paths.get(i_file.getAbsolutePath())), StandardCharsets.UTF_8);
            return content;

        } catch (IOException e)
        {
            throw new Exception("Exception was occured, problem in reading file:" + i_file.getName());
        }
    }

    public String getContent()
    {
        return m_Content;
    }

}

