import java.sql.*;
import java.util.Random;
import java.util.logging.Logger;

public class StudentManagement {
    private static final Logger LOGGER = Logger.getLogger("Student Management");

    public static void addStudent(String userName, int studentId, String firstName,String lastName, Connection conn, ActiveClass activeClass){
        boolean isStudentIdExist = util.checkStudentExist(studentId, conn);
        int classId = activeClass.getClassID();
        String sqlQuery;
        if(isStudentIdExist) {
            LOGGER.info("Checking and updating the student name");
            sqlQuery = String.format(
                    "UPDATE student \n" +
                            "                        SET  \n" +
                            "                            first_name = ?, last_name = ?           \n" +
                            "                            WHERE student_id = ?"

            );
            try {
                String emailAddress = userName + "@u.xyz.edu";
                conn.setAutoCommit(false);
                PreparedStatement statement = conn.prepareStatement(sqlQuery);
                statement.setString(1,firstName);
                statement.setString(2,lastName);
                statement.setInt(3, studentId);
                int rowUpdated = statement.executeUpdate();
                if (rowUpdated > 0)
                    LOGGER.info("Student information updated successfully");
                conn.commit();
                enrollStudent(studentId,classId,conn);
            } catch (SQLException s) {
                throw new RuntimeException(s);
            }
            enrollStudent(studentId, classId, conn);
        }
        else{
            String emailAddress = userName + "@u.xyz.edu";
            Random random = new Random();
            long randomNumber = 1000000000L + random.nextInt(900000000);
            String contactNumber = String.valueOf(randomNumber);
            String address = util.randomAddressGenerator();
            sqlQuery = String.format("INSERT INTO student (student_id, username,first_name,last_name,email_address,contact_number, address)\n" +
                    "                    VALUES(?,?,?,?,?,?,?)"
                   );
            try {
                conn.setAutoCommit(false);
                PreparedStatement statement = conn.prepareStatement(sqlQuery);
                statement.setInt(1, studentId);
                statement.setString(2, userName);
                statement.setString(3, firstName);
                statement.setString(4, lastName);
                statement.setString(5,emailAddress);
                statement.setString(6,contactNumber);
                statement.setString(7,address);

                int rowInserted = statement.executeUpdate();
                if (rowInserted > 0)
                    LOGGER.info("New student successfully inserted");
                conn.commit();
                enrollStudent(studentId,classId,conn);
            } catch (SQLException s) {
                throw new RuntimeException(s);
            }
        }
    }
    public static void addStudent(String userName, Connection conn, ActiveClass activeClass){
        int studentId = util.getStudentId(userName, conn);
        boolean isStudentExist = util.checkStudentExist(studentId,userName,conn);
        if(isStudentExist){
            enrollStudent(studentId,activeClass.getClassID(),conn);
        }
        else
            LOGGER.severe("Student does not exist");
    }

    public static void showStudents(ActiveClass activeClass, Connection conn){
        if(activeClass == null)
            System.out.println("Please select the current class");
        else {
            String sqlQuery = String.format("SELECT s.student_id, s.username, s.first_name,s.last_name FROM student s\n" +
                            "                            JOIN enrolled e\n" +
                            "                                ON s.student_id = e.student_id\n" +
                            "                                WHERE class_id = %d"

                    , activeClass.getClassID()
            );
            try {
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(sqlQuery);
                System.out.println("Student ID\tUsername\tFirst Name\tLast Name");
                System.out.println("------------------------------------------------------------------");
                while (resultSet.next())
                    System.out.println(

                            resultSet.getInt("student_id") + "\t\t"
                                    + resultSet.getString("username") + "\t\t"
                                    + resultSet.getString("first_name") + "\t\t"
                                    + resultSet.getString("last_name")
                    );
            } catch (SQLException s) {
                throw new RuntimeException(s);
            }
        }
    }

    public static void showStudents(String lookupName,ActiveClass activeClass, Connection conn){
        if(activeClass == null)
            LOGGER.severe("Please select the current class");
        else {
            String search = "%" + lookupName.toLowerCase() + "%";
            String sqlQuery = String.format("SELECT s.student_id, s.username, s.first_name, s.last_name FROM student s\n" +
                            "                            JOIN enrolled e\n" +
                            "                                ON s.student_id = e.student_id\n" +
                            "                                WHERE class_id = %d\n" +
                            "                                AND LOWER(s.username) LIKE '%s'"
                    , activeClass.getClassID()
                    , search
            );
            try {
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(sqlQuery);
                System.out.println("Student ID\tUsername\tFirst Name\tLast Name");
                System.out.println("------------------------------------------------------------------");
                while (resultSet.next())
                    System.out.println(
                            resultSet.getInt("student_id") + "\t\t"
                                    + resultSet.getString("username") + "\t\t"
                                    + resultSet.getString("first_name") + "\t\t"
                                    + resultSet.getString("last_name")
                    );
            } catch (SQLException s) {
                throw new RuntimeException(s);
            }
        }
    }

    public static void gradeAssignment(String assignmentName, String username,float grade, Connection conn, ActiveClass activeClass){
        int studentId = util.getStudentId(username,conn);
        if(studentId == -1)
            LOGGER.severe("Student does not exist");
        else {
            int assignmentId = util.getAssignmentId(assignmentName,activeClass.getClassID(),conn);
            if(assignmentId == -1)
                LOGGER.severe("Assignment does not exist");
            else{
                boolean isEnrolled = util.checkStudentEnrolled(studentId,activeClass.getClassID(),conn);
                if(!isEnrolled)
                    LOGGER.severe("Student is not enrolled in course " +activeClass.getCourseNumber());
                else{
                    String sqlQuery = String.format(
                            "INSERT INTO grades(student_id, assignment_id, grade)\n" +
                                    "                                    VALUE (?,?,?)\n" +
                                    "                                       ON DUPLICATE KEY UPDATE grade= ?;"
                                   );
                    try {
                        conn.setAutoCommit(false);
                        PreparedStatement preparedStatement = conn.prepareStatement(sqlQuery);
                        preparedStatement.setInt(1, studentId);
                        preparedStatement.setInt(2, assignmentId);
                        preparedStatement.setFloat(3,grade);
                        preparedStatement.setFloat(4,grade);
                        int rowInserted = preparedStatement.executeUpdate();
                        if (rowInserted > 0) {
                            LOGGER.info("Grades successfully updated");
                            conn.commit();
                            sqlQuery = String.format(
                                   "SELECT g.grade,a.point_value\n" +
                                           "     FROM grades g\n" +
                                           "     INNER JOIN assignment a\n" +
                                           "          \tON g.assignment_id = a.assignment_id\n" +
                                           "                AND a.class_id = %d\n" +
                                           "           WHERE g.student_id = %d\n" +
                                           "                 AND g.assignment_id = %d;"
                                    ,activeClass.getClassID()
                                    ,studentId
                                    ,assignmentId
                            );
                            Statement statement = conn.createStatement();
                            ResultSet resultSet = statement.executeQuery(sqlQuery);
                            resultSet.next();
                            if(resultSet.getRow()>0 && resultSet.getFloat("grade") > resultSet.getFloat("point_value"))
                                LOGGER.warning("Assigned grade is more than the configured points of assignment");
                        }

                    } catch (SQLException s) {
                        throw new RuntimeException(s);
                    }
                }
            }

        }
    }

    public static void enrollStudent(int studentId, int classId, Connection conn){
        boolean isStudentEnrolled = util.checkStudentEnrolled(studentId,classId,conn);
        if(!isStudentEnrolled) {
            String sqlQuery = String.format(
                    "INSERT INTO enrolled(student_id, class_id) VALUES (?,?);");

            try {
                conn.setAutoCommit(false);
                PreparedStatement statement = conn.prepareStatement(sqlQuery);
                statement.setInt(1, studentId);
                statement.setInt(2, classId);
                int rowInserted = statement.executeUpdate();
                if (rowInserted > 0)
                    LOGGER.info("Student successfully enrolled in class");
                conn.commit();
            } catch (SQLException s) {
                throw new RuntimeException(s);
            }
        }
        else
            LOGGER.severe("Student already enrolled");
    }
}
