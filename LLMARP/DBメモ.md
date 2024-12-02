# ER図


```mermaid
erDiagram

repositories |o--|{ commits:""
commits |o--|{ chunks:""

chunks ||--|{ chunk_patterns: ""
patterns ||--|{ chunk_patterns:""

normalization_info ||--|{ chunk_normalization_info:""
chunk_patterns ||--|{ chunk_normalization_info:""

patterns ||--|{ trees :""

patterns ||--|{ pattern_connections :""


repositories{
    INTEGER id PK
    TEXT url
}

commits{
    INTEGER id PK
    INTEGER repository_id
    TEXT hash
    TEXT message
}

chunks{
    INTEGER id PK
    INTEREG commit_id
    TEXT file
    INTEGER old_begin
    INTEGER old_end
    INTEGER new_begin
    INTEGER new_end
    TEXT old_raw
    TEXT new_raw
}

patterns{
    TEXT hash PK
    TEXT old_tree_hash
    TEXT new_tree_hash
    INTEGER is_normalized
    INTEGER is_useful
}

normalization_info{
    TEXT hash PK
    INTEGER type
    INTEGER target_id
    INTEGER order_index
}

trees{
    TEXT hash PK
    TEXT structure
}

chunk_patterns{
    INTEGER id PK
    INTEGER chunk_id
    TEXT pattern_hash
    TEXT normalization_info_hash
}

chunk_normalization_info{
    INTEGER id PK
    TEXT chunk_patterns_id
    TEXT info_hash
}

pattern_connections{
    INTEGER id PK
    TEXT parent_hash
    TEXT child_hash
}

```
