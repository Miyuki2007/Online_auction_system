package model;
import java.io.Serializable;
import java.util.UUID;
public abstract class Entity implements Serializable{
    private static final long serialVersionUID = 1L;
    private final String id;
    protected Entity(){
        this.id = UUID.randomUUID().toString();
    }
    protected Entity(String id){
        this.id = id;
    }
    public String getId() {return id; }
    public abstract String getDisplayInfo();
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id.equals(entity.id);
    }
    @Override
    public int hashCode(){
        return id.hashCode();
    }
}

