package com.ssafy.rescuemungz.pet;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PetMapper {
    @Insert("""
            INSERT INTO pets(user_id, name, breed, age, weight, gender, neutered, allergies, diseases)
            VALUES(#{userId}, #{name}, #{breed}, #{age}, #{weight}, #{gender}, #{neutered}, #{allergies}, #{diseases})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Pet pet);

    @Select("""
            SELECT id, user_id, '강아지' AS species, name, breed, age, weight, gender, neutered,
                   allergies, diseases, created_at, updated_at
            FROM pets
            WHERE user_id = #{userId}
            ORDER BY id DESC
            """)
    List<Pet> findByUserId(long userId);

    @Select("""
            SELECT id, user_id, '강아지' AS species, name, breed, age, weight, gender, neutered,
                   allergies, diseases, created_at, updated_at
            FROM pets
            WHERE id = #{id} AND user_id = #{userId}
            """)
    Pet findByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

    @Select("""
            SELECT id, user_id, '강아지' AS species, name, breed, age, weight, gender, neutered,
                   allergies, diseases, created_at, updated_at
            FROM pets
            WHERE id = #{id}
            """)
    Pet findById(long id);

    @Update("""
            UPDATE pets
            SET name=#{name}, breed=#{breed}, age=#{age}, weight=#{weight},
                gender=#{gender}, neutered=#{neutered}, allergies=#{allergies}, diseases=#{diseases}
            WHERE id=#{id} AND user_id=#{userId}
            """)
    int update(Pet pet);

    @Delete("DELETE FROM pets WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") long id, @Param("userId") long userId);
}
