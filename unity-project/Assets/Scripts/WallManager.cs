using UnityEngine;

public class WallManager : MonoBehaviour
{
    [SerializeField] private Material wallMaterial;

    public void SetWallColor(string hexColor)
    {
        if (ColorUtility.TryParseHtmlString(hexColor, out Color color))
        {
            // Apply to material used for vertical planes
            if (wallMaterial != null)
            {
                wallMaterial.color = new Color(color.r, color.g, color.b, 0.6f); // semi-transparent
            }
        }
    }
}
