using System.Collections.Generic;
using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;

public class ARManager : MonoBehaviour
{
    [SerializeField] private ARPlaneManager planeManager;
    [SerializeField] private ARRaycastManager raycastManager;
    
    private static List<ARRaycastHit> s_Hits = new List<ARRaycastHit>();

    void Awake()
    {
        if (planeManager == null) planeManager = GetComponent<ARPlaneManager>();
        if (raycastManager == null) raycastManager = GetComponent<ARRaycastManager>();
    }

    public void SetPlaneDetection(bool enabled)
    {
        planeManager.enabled = enabled;
        foreach (var plane in planeManager.trackables)
        {
            plane.gameObject.SetActive(enabled);
        }
    }

    public bool GetRaycastHit(Vector2 screenPos, out Pose hitPose)
    {
        if (raycastManager.Raycast(screenPos, s_Hits, TrackableType.PlaneWithinPolygon))
        {
            hitPose = s_Hits[0].pose;
            return true;
        }
        hitPose = Pose.identity;
        return false;
    }
}
