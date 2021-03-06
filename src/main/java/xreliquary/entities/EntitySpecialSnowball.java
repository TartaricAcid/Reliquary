package xreliquary.entities;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import xreliquary.reference.Settings;

import java.util.List;

public class EntitySpecialSnowball extends EntitySnowball {

	public int ticksInAir;
	public int ticksInGround;
	public Block inTile;
	public boolean fromGlacialStaff;
	public int xTile;
	public int yTile;
	public int zTile;

	public EntitySpecialSnowball(World par1World) {
		super(par1World);
	}

	public EntitySpecialSnowball(World par1World, EntityLivingBase par2EntityLiving, boolean b) {
		super(par1World, par2EntityLiving);
		this.setSize(0.01F, 0.01F);
		this.fromGlacialStaff = b;
	}

	public EntitySpecialSnowball(World par1World, double par2, double par4, double par6) {
		super(par1World, par2, par4, par6);
	}

	public int getSnowballDamage() {
		return fromGlacialStaff ? Settings.GlacialStaff.snowballDamage : Settings.IceMagusRod.snowballDamage;
	}

	public int getSnowballDamageFireImmuneBonus() {
		return fromGlacialStaff ? Settings.GlacialStaff.snowballDamageBonusFireImmune : Settings.IceMagusRod.snowballDamageBonusFireImmune;
	}

	public int getSnowballDamageBlazeBonus() {
		return fromGlacialStaff ? Settings.GlacialStaff.snowballDamageBonusBlaze : Settings.IceMagusRod.snowballDamageBonusBlaze;
	}

	/**
	 * Called when this EntityThrowable hits a block or entity.
	 */
	@Override
	protected void onImpact(RayTraceResult result) {
		if(result.entityHit != null) {
			int damage = getSnowballDamage();
			if(result.entityHit.isImmuneToFire())
				damage += getSnowballDamageFireImmuneBonus();
			if(result.entityHit instanceof EntityBlaze) {
				damage += getSnowballDamageBlazeBonus();
			}

			result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), damage);
		}

		if(result.typeOfHit == RayTraceResult.Type.BLOCK && worldObj.getBlockState(result.getBlockPos()).getBlock() == Blocks.FIRE) {
			worldObj.playSound(null, result.getBlockPos().up(), SoundEvents.ENTITY_GENERIC_BURN, SoundCategory.NEUTRAL, 0.5F, (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()) * 0.8F);
			worldObj.setBlockState(new BlockPos(result.getBlockPos().getX(), result.getBlockPos().getY() + 1, result.getBlockPos().getZ()), Blocks.AIR.getDefaultState());
		}

		for(int var3 = 0; var3 < 8; ++var3) {
			worldObj.spawnParticle(EnumParticleTypes.SNOWBALL, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
		}

		if(!worldObj.isRemote) {
			this.setDead();
		}
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	@Override
	public void onUpdate() {
		lastTickPosX = posX;
		lastTickPosY = posY;
		lastTickPosZ = posZ;
		super.onUpdate();

		if(throwableShake > 0) {
			--throwableShake;
		}
		if(ticksInAir % 4 == worldObj.rand.nextInt(5)) {
			worldObj.spawnParticle(EnumParticleTypes.REDSTONE, posX, posY, posZ, 5.0D, 5.0D, 1.0D);
		}
		xTile = (int) Math.round(posX);
		yTile = (int) Math.round(posY);
		zTile = (int) Math.round(posZ);
		inTile = worldObj.getBlockState(new BlockPos(xTile, yTile, zTile)).getBlock();

		if(inGround) {
			Block var1 = worldObj.getBlockState(new BlockPos(xTile, yTile, zTile)).getBlock();

			//TODO: again this condition which will always result in true????
			if(var1 == inTile) {
				++ticksInGround;

				if(ticksInGround == 1200) {
					this.setDead();
				}

				return;
			}

			inGround = false;
			motionX *= rand.nextFloat() * 0.2F;
			motionY *= rand.nextFloat() * 0.2F;
			motionZ *= rand.nextFloat() * 0.2F;
			ticksInGround = 0;
			ticksInAir = 0;
		} else {
			++ticksInAir;
		}

		Vec3d var16 = new Vec3d(posX, posY, posZ);
		Vec3d var2 = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
		RayTraceResult var3 = worldObj.rayTraceBlocks(var16, var2, false, true, false);
		var16 = new Vec3d(posX, posY, posZ);
		var2 = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);

		if(var3 != null) {
			var2 = new Vec3d(var3.hitVec.xCoord, var3.hitVec.yCoord, var3.hitVec.zCoord);
		}

		if(!worldObj.isRemote) {
			Entity var4 = null;
			List<Entity> var5 = worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D));
			double var6 = 0.0D;
			EntityLivingBase var8 = this.getThrower();

			for(Entity var10 : var5) {
				if(var10.canBeCollidedWith() && (var10 != var8 || ticksInAir >= 5)) {
					float var11 = 0.1F;
					AxisAlignedBB var12 = var10.getEntityBoundingBox().expand(var11, var11, var11);
					RayTraceResult var13 = var12.calculateIntercept(var16, var2);

					if(var13 != null) {
						double var14 = var16.distanceTo(var13.hitVec);

						if(var14 < var6 || var6 == 0.0D) {
							var4 = var10;
							var6 = var14;
						}
					}
				}
			}

			if(var4 != null) {
				var3 = new RayTraceResult(var4);
			}
		}

		if(var3 != null) {
			if(var3.typeOfHit == RayTraceResult.Type.BLOCK && worldObj.getBlockState(var3.getBlockPos()).getBlock() == Blocks.PORTAL) {
				this.setPortal(var3.getBlockPos());
			} else {
				this.onImpact(var3);
			}
		}

		posX += motionX;
		posY += motionY;
		posZ += motionZ;
		float var17 = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
		//noinspection SuspiciousNameCombination
		rotationYaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);

		//noinspection StatementWithEmptyBody
		for(rotationPitch = (float) (Math.atan2(motionY, var17) * 180.0D / Math.PI); rotationPitch - prevRotationPitch < -180.0F; prevRotationPitch -= 360.0F) {

		}

		while(rotationPitch - prevRotationPitch >= 180.0F) {
			prevRotationPitch += 360.0F;
		}

		while(rotationYaw - prevRotationYaw < -180.0F) {
			prevRotationYaw -= 360.0F;
		}

		while(rotationYaw - prevRotationYaw >= 180.0F) {
			prevRotationYaw += 360.0F;
		}

		rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
		rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
		float var18 = 0.99F;
		float var19 = this.getGravityVelocity();

		if(this.isInWater()) {
			for(int var7 = 0; var7 < 4; ++var7) {
				float var20 = 0.25F;
				worldObj.spawnParticle(EnumParticleTypes.WATER_BUBBLE, posX - motionX * var20, posY - motionY * var20, posZ - motionZ * var20, motionX, motionY, motionZ);
			}

			var18 = 0.8F;
		}

		motionX *= var18;
		motionY *= var18;
		motionZ *= var18;
		motionY -= var19;
		this.setPosition(posX, posY, posZ);
	}

	/**
	 * Gets the amount of gravity to apply to the thrown entity with each tick.
	 */
	@Override
	protected float getGravityVelocity() {
		// flies slightly farther than a normal projectile;
		return 0.01F;
	}
}
