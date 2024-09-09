class ChunkData:
    def __init__(self,name:str,data:list[str]):
        self.name = name
        self.data = data

    def from_json(json_data:dict):
        name = json_data["name"]
        data = json_data["data"]
        return ChunkData(name,data)
    
    def to_json(self):
        return {
            "name" : self.name,
            "data" : self.data
        }

class RawPatchData:
    def __init__(self,file_name:str,chunks:list[ChunkData]):
        self.file_name = file_name
        self.chunks = chunks
    
    def from_json(json_data:dict):
        file_name = json_data["file_name"]
        raw_chunks = json_data["chunks"]
        chunks = []
        for c in raw_chunks:
            chunks.append(ChunkData.from_json(c))
        return RawPatchData(file_name,chunks)

    def to_json(self):
        return {
            "file_name" : self.file_name,
            "chunks" : [c.to_json() for c in self.chunks]
        }

class RawPatchCommit:
    def __init__(self,commit_has:str,url:str):
        self.commit_hash = commit_has
        self.url = url
        self.patch = []

    def from_json(json_data:dict):
        commit_hash = json_data["commit_hash"]
        url = json_data["url"]
        raw_patch = json_data["patch"]
        patch = []
        for p in raw_patch:
            patch.append(RawPatchData.from_json(p))

        result =  RawPatchCommit(commit_hash,url)
        result.patch = patch
        return result

    def to_json(self):
        return {
            "commit_hash" : self.commit_hash,
            "url" : self.url,
            "patch" : [p.to_json() for p in self.patch]
        }
    
    def add_patch(self,patch:RawPatchData):
        self.patch.append(patch)