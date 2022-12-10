package com.techelevator.dao.jdbc;


import com.techelevator.dao.GroupDao;
import com.techelevator.dao.UserDao;
import com.techelevator.dao.UtilDao;
import com.techelevator.dao.exceptions.CreateException;
import com.techelevator.dao.exceptions.DeleteException;
import com.techelevator.dao.exceptions.GetException;
import com.techelevator.model.Group;
import com.techelevator.model.GroupMember;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class JdbcGroupDao implements GroupDao {
    private final JdbcTemplate jdbcTemplate;
    private UtilDao utilDao;
    private UserDao userDao;

    public JdbcGroupDao(DataSource dataSource, UtilDao utilDao, UserDao userDao) {
        this.jdbcTemplate = new JdbcTemplate((dataSource));
        this.utilDao = utilDao;
        this.userDao = userDao;
    }

    @Override
    public void createGroup(Group group) {
        group.setGroupCode(getGroupCode());
        Integer groupId;
        String sql = "INSERT INTO groups (group_owner, group_name, group_code, description) values (?, ?, ?, ?) RETURNING group_id";
        try {
            groupId = jdbcTemplate.queryForObject(sql, Integer.class, group.getGroupOwnerId(), group.getGroupName(), group.getGroupCode(), group.getGroupDescription());
        } catch (DataAccessException e) {
            throw new CreateException(e);
        }

        String sqlUser = "INSERT INTO group_member (group_id, user_id, date_joined) values (?, ?, ?)";
        try {
            jdbcTemplate.update(sqlUser, groupId, group.getGroupOwnerId(), utilDao.currentDay());
        } catch (DataAccessException e) {
            throw new CreateException("group member failed admission test");
        }
    }

    @Override
    public void deleteGroup(int groupId) {
        String sql = "DELETE FROM groups WHERE group_id = ?;";
        try {
            jdbcTemplate.update(sql, groupId);
        } catch (DataAccessException e) {
            throw new DeleteException(e);
        }
        sql = "DELETE FROM group_member WHERE group_id = ?";
        jdbcTemplate.update(sql, groupId);
    }

    //TODO only allow owner to edit
    @Override
    public void editGroup(Group group) {
        String sql = "UPDATE groups set description = ?, group_name = ? WHERE group_id = ?";
        try {
            jdbcTemplate.update(sql, group.getGroupDescription(), group.getGroupName(), group.getGroupId());
        } catch (DataAccessException e) {
            throw new GetException(e);
        }
    }

    //TODO add validation so only user that has access to the group can access a group
    public Group getGroupById(int groupId) {
        String sql = "SELECT * FROM groups WHERE group_id = ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, groupId);
        try {
            if (rowSet.next()) {
                return mapRowToGroup(rowSet);
            }
        } catch (DataAccessException e) {
            throw new GetException(e);
        }
        return new Group();
    }


    @Override
    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT * FROM groups ORDER BY group_name";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while (results.next()) {
            Group group = mapRowToGroup(results);
            groups.add(group);
        }
        return groups;
    }

    //TODO currentday is set in controller needs to be done
    @Override
    public void addUserToGroup(GroupMember groupMember) {
        String sql = "INSERT INTO group_member (group_id, user_id, date_joined) values (?, ?, ?)";
        try {
            jdbcTemplate.update(sql, groupMember.getGroupId(), groupMember.getMemberId(), groupMember.getDateJoined());
        } catch (DataAccessException e) {
            throw new CreateException(e);
        }
    }

    @Override
    public List<GroupMember> getAllMembers(int groupId) {
        List<GroupMember> allMembers = new ArrayList<>();
        String sql = "SELECT * FROM group_member WHERE group_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, groupId);
        while (results.next()) {
            GroupMember groupMember = mapRowToMemberGroup(results);
            allMembers.add(groupMember);
        }
        return allMembers;
    }
    @Override
    public boolean isMemberInGroupByUsername(int groupId, String username){
        int memberId = userDao.findIdByUsername(username);
        GroupMember groupMember = new GroupMember();
        String sql = "SELECT * FROM group_member WHERE user_id = ? AND group_id =?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, memberId, groupId);
        return results.next();
    }

    //TODO: implement
    @Override
    public void removeUserFromGroup(int userId, int groupId){
        String sql = "DELETE FROM group_member WHERE user_id = ? AND group_id = ?;";
        try {
            jdbcTemplate.update(sql, userId, groupId);
        } catch (DataAccessException e) {
            throw new DeleteException(e);
        }
    }


    private Group mapRowToGroup(SqlRowSet rs) {
        Group group = new Group();
        group.setGroupName(rs.getString("group_name"));
        group.setGroupId(rs.getInt("group_id"));
        group.setGroupOwnerId(rs.getInt("group_owner"));
        group.setGroupCode(rs.getString("group_code"));
        group.setGroupDescription(rs.getString("description"));
        return group;
    }

    private GroupMember mapRowToMemberGroup(SqlRowSet rs) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroupId(rs.getInt("group_id"));
        groupMember.setMemberId(rs.getInt("user_id"));
        groupMember.setDateJoined(rs.getString("date_joined"));
        return groupMember;
    }

    private String getGroupCode() {
        char[] chars = new char[]{'a', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
                'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        String groupCode = "";
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            int randomInt = r.nextInt(35);
            groupCode += chars[randomInt];
        }
        return groupCode;

    }
//    boolean isVerified(String username, int groupId){
//        String sql = "SELECT * FROM group_member where group_id = ? and user_id = ?";
//        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, groupId, userDao.findIdByUsername(username));
//        return results.next();
//    }
}
