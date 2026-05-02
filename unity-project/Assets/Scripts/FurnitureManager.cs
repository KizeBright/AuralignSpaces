using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.XR.ARFoundation;

public class FurnitureManager : MonoBehaviour
{
    private Dictionary<string, GameObject> placedObjects = new Dictionary<string, GameObject>();

    public void PlaceObject(string json)
    {
        // Simple DTO for Unity
        PlacedObjectDto dto = JsonUtility.FromJson<PlacedObjectDto>(json);
        GameObject prefab = Resources.Load<GameObject>("Models/" + dto.itemId);
        if (prefab != null)
        {
            GameObject obj = Instantiate(prefab, new Vector3(dto.posX, dto.posY, dto.posZ), Quaternion.Euler(0, dto.rotationDeg, 0));
            obj.transform.localScale = Vector3.one * dto.scale;
            placedObjects.Add(dto.instanceId, obj);
        }
    }

    public void RemoveObject(string instanceId)
    {
        if (placedObjects.TryGetValue(instanceId, out GameObject obj))
        {
            Destroy(obj);
            placedObjects.Remove(instanceId);
        }
    }

    public void RotateObject(string instanceId, float degrees)
    {
        if (placedObjects.TryGetValue(instanceId, out GameObject obj))
        {
            obj.transform.Rotate(Vector3.up, degrees);
        }
    }

    public void ScaleObject(string instanceId, float scale)
    {
        if (placedObjects.TryGetValue(instanceId, out GameObject obj))
        {
            obj.transform.localScale = Vector3.one * scale;
        }
    }

    [System.Serializable]
    public class PlacedObjectDto
    {
        public string instanceId;
        public string itemId;
        public float posX;
        public float posY;
        public float posZ;
        public float rotationDeg;
        public float scale;
    }
}
