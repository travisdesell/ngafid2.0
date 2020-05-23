package org.ngafid.common;

import java.lang.String;

public class Tag<E>{
    //the key in the database
    private int hashId;
    private String name, description;

    /**
     * Creates an instance of a tag
     * @param hash the id in SQL
     * @param name the user specified name
     * @param description the user specified description of the tag
     */
    public Tag(int hash, String name, String description){
        this.hashId = hash;
        this.name = name;
        this.description = description;
    }

    /**
     * The hashcode of a tag is its key in the database
     * @return an int with the key/hashcode
     */
    @Override
    public int hashCode(){
        return this.hashId;
    }

    /**
     * Checks for equality between two tags
     * @return a bool representing the reltionship between two tags
     */
    // @Override
    // public Boolean equals(Object other){
    //     if(other instanceOf Tag<>){
    //         Tag<> t = (Tag)other;
    //         return t.hashCode() == this.hashCode();
    //     }
    //     return false;
    // }
}

