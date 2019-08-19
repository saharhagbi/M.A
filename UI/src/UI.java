import System.Branch;
import System.Engine;
import XmlObjects.MagitRepository;
import XmlObjects.XMLMain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;

public class UI
{
    public static final String sf_NoRepositoryExistMsg =
            "Repository doesn't exist in system yet";

    private Engine systemEngine = new Engine();
    private XMLMain m_XMLMain = new XMLMain();

    public void start()
    {
        int userChoice;
        Scanner menuInteger = new Scanner(System.in);

        System.out.println("first change for merging");

        showMenu();
        userChoice = menuInteger.nextInt();

        while ((userChoice >= 1) && (userChoice <= 12))
        {
            try
            {
                handleUserMenuChoice(userChoice);
            } catch (Exception e)
            {
                System.out.println("Exception Occured!" + System.lineSeparator() + e.getMessage() + System.lineSeparator());
            }

            showMenu();
            userChoice = menuInteger.nextInt();

        }
    }

    private void handleUserMenuChoice(int i_UserChoice) throws Exception
    {
        String repositoryName;
        String pathToRepository;


        switch (i_UserChoice)
        {
            //Update new user on system
            case 1:
                String userName = getString("Enter your user name");
                systemEngine.UpdateNewUserInSystem(userName);
                break;

            //Reading repository from XML
            case 2:
                loadRepositoryFromXML();
                break;

            //Creating local repository on computer
            case 3:
                pathToRepository = getString("Enter your path repository");
                repositoryName = getString("Enter you repository name");
                Path repositoryAskedPath = Paths.get(pathToRepository);
                try
                {
                    systemEngine.CreateNewLocalRepository(repositoryAskedPath, repositoryName);
                } catch (Exception e)
                {
                    System.out.println("Exception:" + System.lineSeparator() + e.getMessage());
                }
                break;

            //Replace current repository in a new one
            case 4:
                pathToRepository = getString("Enter your path repository");
                repositoryName = getString("Enter you repository name");
                replaceCurrentRepository(repositoryName, pathToRepository);
                break;

            //Showing all files of current commit including all history data
            case 5:
                systemEngine.CheckIfRepoAndCommitInSystem();
                showAllFilesOfCurretCommit();
                break;
            //Show status on WC
            case 6:
                systemEngine.CheckIfRepoAndCommitInSystem();
                String messageOfShowStatus = systemEngine.ShowStatus();
                System.out.println(messageOfShowStatus);
                break;
            //Commit
            case 7:
                systemEngine.CheckExistenceCurrentRepository();
                commitChangesInRepository();
                break;
            //Showing all branches
            case 8:
                systemEngine.CheckIfRepoAndCommitInSystem();
                showAllBranches();
                break;
            //Creating new branches
            case 9:
                systemEngine.CheckIfRepoAndCommitInSystem();
                String newBranchNameToAdd = getString("Enter Branch Name");
                systemEngine.CreateNewBranchToSystem(newBranchNameToAdd);
                break;
            //Delete Branches
            case 10:
                systemEngine.CheckIfRepoAndCommitInSystem();
                String getAllBranchesName = this.systemEngine.getCurrentRepository().getAllBranchesName();
                System.out.println("You can choose from the below:\n" + getAllBranchesName);
                String branchNameToErase = getString("Enter Branch Name");
                systemEngine.DeleteBranchFromSystem(branchNameToErase);
                break;

            //CheckOut - choose another branch
            case 11:
                systemEngine.CheckIfRepoAndCommitInSystem();
                checkout();
                break;
            //Showing history of active Branches
            case 12:
                systemEngine.CheckIfRepoAndCommitInSystem();
                this.systemEngine.GetCommitHistoryInActiveBranch();
                break;

            //Exit
            default:
                if (systemEngine.getCurrentRepository() != null)
                    this.systemEngine.RemoveTempFolder();
                System.out.println("Exit from system");
                break;

        }
    }

    private void checkExecutionOfCurrentAction() throws Exception
    {
        if ((systemEngine.getCurrentRepository() == null) ||
                (systemEngine.getCurrentRepository().ThereAreNoCmmitsYet()))
            throw new Exception(sf_NoRepositoryExistMsg);
    }

    private void loadRepositoryFromXML() throws Exception
    {
        boolean isXMLRepoExist;

        String userPath = getString("Enter the path of your xml file");
        isXMLRepoExist = m_XMLMain.CheckXMLFile(Paths.get(userPath));

        if (!isXMLRepoExist)
            systemEngine.setCurrentRepository(
                    m_XMLMain.ParseAndWriteXML(m_XMLMain.GetXmlRepository())
            );
        else
            handleCurrentRepositoryAlreadyExist(m_XMLMain.GetXmlRepository());
    }

    private void handleCurrentRepositoryAlreadyExist(MagitRepository i_MagitRepository) throws Exception
    {
        Scanner repoInput = new Scanner(System.in);

        System.out.println("Repository Already exist in this location." + System.lineSeparator()
                + "Choose an option:" + System.lineSeparator()
                + "1. Load Repository from xml anyway" + System.lineSeparator()
                + "2. Move to The repository in the location you entered" + System.lineSeparator());

        int repoChoice = repoInput.nextInt();
        systemEngine.ExecuteUserChoice(repoChoice, m_XMLMain.GetXmlRepository(), m_XMLMain);
    }

    private void showAllFilesOfCurretCommit() throws Exception
    {
        String commitsData = this.systemEngine.ShowAllCurrentCommitData();
        System.out.println(commitsData);
    }

    private void commitChangesInRepository() throws Exception
    {
        if (this.systemEngine.getCurrentRepository().ThereAreNoCmmitsYet())
        {
            String messageOfCommit = getString("Enter your commit message");
            systemEngine.CommitInCurrentRepository(messageOfCommit);
        } else
        {
            if (systemEngine.ShowStatus() == Engine.sf_NoChangesMsg)
            {
                System.out.println("Nothing to commit on" + System.lineSeparator());
            } else
            {

                String messageOfCommit = getString("Enter your commit message");
                systemEngine.CommitInCurrentRepository(messageOfCommit);
            }
        }

    }

    private void replaceCurrentRepository(String repositoryName, String pathToRepository)
    {
        try
        {
            systemEngine.PullAnExistingRepository(pathToRepository, repositoryName);
        } catch (FileNotFoundException e)
        {
            System.out.println(e.getMessage());
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (ParseException e)
        {
            e.printStackTrace();
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void checkout() throws Exception
    {
        String getAllBranchesName = this.systemEngine.getCurrentRepository().getAllBranchesName();
        System.out.println("You can choose from the below:\n" + getAllBranchesName);

        if (this.systemEngine.CheckIfRootFolderChanged() == false)
        {
            getBranchNameAndCheckOut();
        } else
        {
            String answer = getString("There were changes since last commit" + System.lineSeparator()
                    + "Are you sure you want to continue?");
            handleUserChoice(answer);
        }
    }

    private void getBranchNameAndCheckOut() throws Exception
    {
        String branchName = getString("Enter name of Branch");
        this.systemEngine.CheckOut(branchName);
    }

    private void showMenu()
    {
        System.out.println("=====================================================" +
                "Choose one of the following options: \n \n" +
                "1. Update new user name on system \n" +
                "2. Reading details on new System.Repository from XML file \n" +
                "3. Create new local System.Repository \n" +
                "4. switch current System.Repository \n" +
                "5. Showing all files of current commit including historic information\n" +
                "6. Show status - Showing current WC\n" +
                "7. Commit Changes\n" +
                "8. Showing all branches in system\n" +
                "9. Creating new branch\n" +
                "10. Delete branch\n" +
                "11. Choosing new head branch (Check-out)  \n" +
                "12. Showing history of active branch \n" +
                "13. Exit\n" +
                "====================================================="
        );
    }

    private String getString(String i_Prompt)
    {
        Scanner inputString = new Scanner(System.in);

        System.out.println(i_Prompt);
        return inputString.nextLine();
    }

    public void showAllBranches() throws Exception
    {
        checkExecutionOfCurrentAction();

        List<Branch> allBranchesForShowingData = systemEngine.getCurrentRepository().getAllBranches();

        for (Branch currentBranch : allBranchesForShowingData)
        {
            showingOneBranchData(currentBranch);
        }
    }

    private void showingOneBranchData(Branch i_BranchToShow)
    {
        System.out.println("Branch Name: " + i_BranchToShow.getBranchName() + System.lineSeparator());
        System.out.println("The SHA1 of pointed commit is: " + i_BranchToShow.getCurrentCommit().getSHA1());
        System.out.println("Message of the pointedCommit is: " + i_BranchToShow.getCurrentCommit().getCommitMessage() + System.lineSeparator());
    }

    private void handleUserChoice(String i_Answer) throws Exception
    {
        i_Answer = i_Answer.toUpperCase();
        switch (i_Answer)
        {
            case "YES":
                break;

            case "NO":
                System.out.println("Ok, So let's commit first");
                commitChangesInRepository();
                break;

            default:
                throw new Exception("Invalid answer, please type Yes or No only!");
        }
        getBranchNameAndCheckOut();
    }
}
