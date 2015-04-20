package ca.licef.comete.security;

import java.util.HashMap;
import java.util.Map;

public enum Role {

    ADMIN( "admin" ), PUBLISHER( "publisher" ), CONTRIBUTOR( "contributor" ), NONE( "none" );
    
    private Role( String value ) {
        this.value = value;
    }

    public String getValue() {
        return( value );
    }

    public static Role get( String code ) { 
        if( codeToRoleMapping == null )
            initMapping();
        return( codeToRoleMapping.get( code ) ); 
    }

    private static void initMapping() {
        codeToRoleMapping = new HashMap<String,  Role>();
        for( Role role : values() )
            codeToRoleMapping.put( role.value, role );
    } 
    
    private String value;
    private static Map<String, Role> codeToRoleMapping;

}


