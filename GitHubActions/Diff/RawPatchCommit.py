class RawPatchData:
    def __init__(self,file_name:str,vector:list[str]):
        self.file_name = file_name
        self.vector = vector
    
    def from_json(json_data:dict):
        file_name = json_data["file_name"]
        vector = json_data["vector"]
        return RawPatchData(file_name,vector)

    def to_json(self):
        return {
            "file_name" : self.file_name,
            "vector" : self.vector
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