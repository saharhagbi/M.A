package System;

import Objects.Commit;
import Objects.Folder;
import Objects.Item;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Branch
{
    private String m_BranchName;
    private Commit m_CurrentCommit;
    private Commit m_PrevCommit;

    public Branch(String i_BranchName, Commit i_CurrentCommit)
    {
        m_CurrentCommit = i_CurrentCommit;
        m_BranchName = i_BranchName;
    }

    public static String GetCommitHistory(Branch i_Branch, Path i_ObjectsFolder) throws IOException
    {
        StringBuilder commitHistoryBuilder = new StringBuilder();
        String headline = "Commits details:\n";
        commitHistoryBuilder.append(headline);
        commitHistoryBuilder.append(Commit.GetInformation(i_Branch.getCurrentCommit()));
        if (i_Branch.getCurrentCommit().GetPrevSha1() != null)
        {
            if (!i_Branch.getCurrentCommit().GetPrevSha1().equals("null"))
            {
                commitHistoryBuilder.append("Previous Commit:\n");
                Path prevCommitTextFileZipped = Paths.get(i_ObjectsFolder.toString() + "\\" + i_Branch.m_CurrentCommit.GetPrevSha1());
                Path PrevCommitTextFileUnzipped = Item.UnzipFile(prevCommitTextFileZipped, Paths.get(i_ObjectsFolder.getParent().toString() + "\\Temp"));
                String prevCommitsDetails = Commit.GetInformationFromCommitTextFile(i_Branch.m_CurrentCommit.GetPrevSha1(), PrevCommitTextFileUnzipped, i_ObjectsFolder);
                commitHistoryBuilder.append(prevCommitsDetails);
            }
        }
        return commitHistoryBuilder.toString();
    }

    public static List<Branch> GetAllBranches(Path i_BranchFolderPath) throws Exception
    {
        List<Branch> allBranches = new ArrayList<Branch>();
        File[] allBranchesFiles = i_BranchFolderPath.toFile().listFiles();
        for (int i = 0; i < allBranchesFiles.length; i++)
        {
            if (!allBranchesFiles[i].getName().equals("HEAD.txt"))
            {
                allBranches.add(Branch.createBranchInstanceFromExistBranch(allBranchesFiles[i].toPath()));
            }
        }
        return allBranches;
    }

    public static Branch createBranchInstanceFromExistBranch(Path i_BranchesPath) throws Exception
    {
        String branchName;
        Path realPathToBranch = i_BranchesPath;
        Path workingCopyPath = i_BranchesPath.getParent().getParent().getParent();
        // 1. get branch name
        // if the path is to the HEAD Branch then we want to extract the real branch name
        if (realPathToBranch.getFileName().getFileName().toString().equals("HEAD.txt"))
        {
            branchName = extractBranchName(i_BranchesPath);
            realPathToBranch = Paths.get(i_BranchesPath.getParent().toString() + "\\" + branchName + ".txt");
        } else
        {
            String[] fileNameAndExtension = i_BranchesPath.getFileName().toString().split("\\.(?=[^\\.]+$)");
            branchName = fileNameAndExtension[0];
        }

        String branchCommitsSha1 = Branch.getCommitSha1FromBranchFile(realPathToBranch);
        Path ObjectsFolderPath = Paths.get(realPathToBranch.getParent().getParent().toString() + "\\Objects");
        Path commitsPath = Paths.get(ObjectsFolderPath.toString() + "\\" + branchCommitsSha1);

        //get tempFolder path
        Path tempFolderPath = Paths.get(i_BranchesPath.getParent().getParent().toString() + "\\Temp");
        if (!tempFolderPath.toFile().exists())
        {
            tempFolderPath.toFile().mkdir();
        }
        Path tempUnzippedCommitTextPath = Item.UnzipFile(commitsPath, tempFolderPath);
        String[] CommitsFields = Commit.GetCommitFieldsFromCommitTextFile(tempUnzippedCommitTextPath);

        String[] rootFolderDetails = Item.GetItemsDetails(CommitsFields[0]);
        String rootFolderSha1 = rootFolderDetails[1];
        String sha1OfLastCommit = CommitsFields[1];
        String message = CommitsFields[2];
        User rootFolderUser = new User(rootFolderDetails[3]);
        User commitUser = new User(CommitsFields[4]);

        Date commitsDate = Item.ParseDateWithFormat(CommitsFields[3]);
        Path WCTextFileZipped = Paths.get(ObjectsFolderPath.toString() + "\\" + rootFolderSha1);
        Path WCTextFileUnzippedPath = Item.UnzipFile(WCTextFileZipped, tempFolderPath);
        Folder commitsRootFolder = Folder.CreateFolderFromTextFolder(WCTextFileUnzippedPath.toFile(), workingCopyPath, rootFolderSha1, rootFolderUser, commitsDate, ObjectsFolderPath);
        Commit branchCommit = new Commit(branchCommitsSha1, commitsRootFolder, sha1OfLastCommit, message, commitUser, commitsDate);

        return new Branch(branchName, branchCommit);
    }


    private static String getCommitSha1FromBranchFile(Path i_Branch) throws FileNotFoundException
    {
        String commitsSha1 = null;
        Scanner lineScanner = new Scanner(i_Branch.toFile());
        while (lineScanner.hasNext())
        {
            commitsSha1 = lineScanner.nextLine();
        }
        return commitsSha1;
    }

    private static String extractBranchName(Path i_branchesPath) throws FileNotFoundException
    {
        Scanner lineScanner = new Scanner(i_branchesPath.toFile());
        String branchName = null;
        while (lineScanner.hasNext())
        {
            branchName = lineScanner.next();

        }
        return branchName;
    }

    public String getBranchName()
    {
        return m_BranchName;
    }

    public Commit getCurrentCommit()
    {
        return m_CurrentCommit;
    }

    public Commit getPrevCommit()
    {
        return this.m_PrevCommit;
    }

    public void SetPrevCommit(Commit i_Commit)
    {
        m_PrevCommit = i_Commit;
    }

    public void SetCurrentCommit(Commit i_Commit)
    {
        m_CurrentCommit = i_Commit;
    }
}


