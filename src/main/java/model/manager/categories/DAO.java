package model.manager.categories; // Hoặc package dao;
import java.util.List;

public interface DAO<T> {
    List<T> getAll();
    T getById(int id);
    boolean insert(T t);
    boolean update(T t);
    boolean delete(int id);
}