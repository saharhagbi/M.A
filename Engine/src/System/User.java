package System;

public class User
{
    private String m_UserName = "Administrator";

    public User(String i_UserName)
    {
        m_UserName = i_UserName;
    }

    public String getUserName()
    {
        return m_UserName;
    }

    public void setUserName(String i_UserName)
    {
        m_UserName = i_UserName;
    }
}
