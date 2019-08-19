package Objects;

import System.User;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Commit
{
    private Folder m_RootFolder;
    private String m_SHA1;
    private String m_PrevCommitSha1 = null;
    private String m_CommitMessage;
    private User m_UserCreated;
    private Date m_Date;

    public Commit(String i_CommitsSha1, Folder i_RootFolder, String i_SHA1PrevCommit, String i_CommitMessage, User i_UserCreated, Date i_Date)
    {
        this.m_SHA1 = i_CommitsSha1;
        this.m_RootFolder = i_RootFolder;
        this.m_PrevCommitSha1 = i_SHA1PrevCommit;
        this.m_CommitMessage = i_CommitMessage;
        this.m_UserCreated = i_UserCreated;
        this.m_Date = i_Date;
    }


    public static String[] GetCommitFieldsFromCommitTextFile(Path i_CommitTextFilePath) throws IOException
    {
        Scanner lineScanner = new Scanner(i_CommitTextFilePath);
        String[] commitTextFileFields = new String[5];
        int i = 0;
        while (lineScanner.hasNext())
        {
            commitTextFileFields[i] = lineScanner.nextLine();
            i++;
        }
        return commitTextFileFields;
    }

    public static String GetInformation(Commit i_Commit)
    {
        String dateFormatted = Item.getDateStringByFormat(i_Commit.m_Date);
        StringBuilder commitInfo = new StringBuilder();
        commitInfo.append("Sha1: " + i_Commit.m_SHA1 + "\n");
        commitInfo.append("Message: " + i_Commit.m_CommitMessage + "\n");
        commitInfo.append("Date: " + dateFormatted + "\n");
        commitInfo.append("User: " + i_Commit.m_UserCreated.getUserName() + "\n");
        return commitInfo.toString();
    }

    public static String GetInformationFromCommitTextFile(String i_commitsSha1, Path i_commitTextFileUnzipped, Path i_ObjectsFolderPath) throws IOException
    {
        StringBuilder commitHistoryBuilder = new StringBuilder();
        String headline = "Commits details:" + System.lineSeparator();
        commitHistoryBuilder.append(headline);
        String[] commitsDetails = Commit.GetCommitFieldsFromCommitTextFile(i_commitTextFileUnzipped);
        String[] rootFolderFields = Item.GetItemsDetails(commitsDetails[0]);
        commitHistoryBuilder.append("Sha1: " + i_commitsSha1 + System.lineSeparator());
        commitHistoryBuilder.append("Message: " + commitsDetails[2] + System.lineSeparator());
        commitHistoryBuilder.append("Date: " + commitsDetails[3] + System.lineSeparator());
        commitHistoryBuilder.append("User: " + commitsDetails[4] + System.lineSeparator() + System.lineSeparator());

        if (!commitsDetails[1].equals("null"))
        {
            Path prevCommitTextFileZipped = Paths.get(i_ObjectsFolderPath.toString() + "\\" + commitsDetails[1]);
            Path prevCommitTextFileUnzipped = Item.UnzipFile(prevCommitTextFileZipped, Paths.get(i_ObjectsFolderPath.getParent().toString() + "\\Temp"));
            commitHistoryBuilder.append("Previous Commit:" + System.lineSeparator());
            commitHistoryBuilder.append(GetInformationFromCommitTextFile(commitsDetails[1], prevCommitTextFileUnzipped, i_ObjectsFolderPath));
        }
        return commitHistoryBuilder.toString();

    }

    public static String createSha1ForCommit(Folder i_rootFolder, String i_sha1PrevCommit, String i_commitMessage, User i_user, Date date)
    {
        StringBuilder strForCalculatingSHA1 = new StringBuilder();
        strForCalculatingSHA1.append(i_rootFolder.getSHA1());
        strForCalculatingSHA1.append(i_sha1PrevCommit);
        strForCalculatingSHA1.append(i_commitMessage);

        return DigestUtils.sha1Hex(strForCalculatingSHA1.toString());
    }

    public static String findDifferences(Commit i_LatestCommit, Commit i_OtherCommit)
    {
        String changesBetweenThisCommitAndTheOther;
        if (!i_LatestCommit.getSHA1().equals(i_OtherCommit.m_RootFolder.getSHA1()))
        {
            changesBetweenThisCommitAndTheOther = Folder.FinedDifferences(i_LatestCommit.m_RootFolder, i_OtherCommit.m_RootFolder);
        } else
        {
            changesBetweenThisCommitAndTheOther = "No Changes have Been Made";
        }
        return changesBetweenThisCommitAndTheOther;
    }

    public Folder getRootFolder()
    {
        return m_RootFolder;
    }

    public String getSHA1()
    {
        return m_SHA1;
    }

    public String getCommitMessage()
    {
        return m_CommitMessage;
    }

    public String CreatingContentOfCommit() throws ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");

        StringBuilder rootFolderStringBuilder = new StringBuilder();
        rootFolderStringBuilder.append(this.m_RootFolder.getName() + ",");//Name
        rootFolderStringBuilder.append(this.m_RootFolder.getSHA1() + ",");//sha1
        rootFolderStringBuilder.append(this.m_RootFolder.getTypeOfFile().toString() + ",");//type
        rootFolderStringBuilder.append(this.m_RootFolder.getUser().getUserName() + ",");//user
        rootFolderStringBuilder.append(dateFormat.format(this.m_RootFolder.getDate()) + "\n");//date

        //example: 123,50087888a7c34344416ec0fd600f394dadf3d9d8,FOLDER,Administrator,06.39.2019-06:39:27:027
        StringBuilder contentOfCommitTextFile = new StringBuilder(rootFolderStringBuilder);//[0]rootFolder line of details name,sha1,type,user,date
        contentOfCommitTextFile.append(m_PrevCommitSha1 + '\n');//[1]prevCommit sha1
        contentOfCommitTextFile.append(m_CommitMessage + '\n');//[2]message
        contentOfCommitTextFile.append(dateFormat.format(m_Date) + "\n");//[3]date
        contentOfCommitTextFile.append(m_UserCreated.getUserName());//[4]user

        return contentOfCommitTextFile.toString();
    }

    public String GetPrevSha1()
    {
        return this.m_PrevCommitSha1;
    }

    public String getAllFolderAndBlobsData()
    {
        StringBuilder commitsDataBuilder = new StringBuilder();
        commitsDataBuilder.append("All data of commit:\n");
        String commitDetails = Commit.GetInformation(this);
        commitsDataBuilder.append("Commit details:");
        commitsDataBuilder.append(commitDetails);
        commitsDataBuilder.append("Root folder information:\n");
        String rootFolderInformation = Folder.GetInformation(this.m_RootFolder);
        commitsDataBuilder.append(rootFolderInformation);
        return commitsDataBuilder.toString();
    }

    public User getUserCreated()
    {
        return m_UserCreated;
    }

    public String getPrevCommitSha1()
    {
        return m_PrevCommitSha1;
    }
}
